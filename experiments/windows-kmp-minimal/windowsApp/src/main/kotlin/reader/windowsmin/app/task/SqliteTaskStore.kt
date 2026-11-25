package reader.windowsmin.app.task

import reader.windowsmin.shared.task.TaskRecord
import reader.windowsmin.shared.task.TaskState
import reader.windowsmin.shared.task.TaskStore
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * W-E07: SQLite-backed persistent task store for the Windows client.
 * Idempotent enqueue, lease-based claim, retry with max attempts, cancellation,
 * and recovery of expired leases after a process restart.
 */
class SqliteTaskStore(private val dbFile: File) : TaskStore, AutoCloseable {

    private val connection: Connection = DriverManager.getConnection(
        "jdbc:sqlite:${dbFile.absolutePath}",
        java.util.Properties().apply {
            setProperty("busy_timeout", "5000")
            setProperty("transaction_mode", "IMMEDIATE")
        },
    )

    init {
        connection.createStatement().use { st ->
            st.execute("PRAGMA busy_timeout = 5000")
            st.execute("PRAGMA journal_mode = WAL")
            st.execute("PRAGMA synchronous = FULL")
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    state TEXT NOT NULL,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    lease_until INTEGER,
                    last_error TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    @Synchronized
    override fun enqueue(taskId: String, type: String, payload: String): Boolean {
        val exists = find(taskId) != null
        if (exists) return false
        connection.prepareStatement(
            "INSERT INTO tasks(task_id, type, payload, state, attempts, lease_until, last_error, updated_at) " +
                "VALUES(?, ?, ?, ?, 0, NULL, NULL, ?)",
        ).use { ps ->
            ps.setString(1, taskId)
            ps.setString(2, type)
            ps.setString(3, payload)
            ps.setString(4, TaskState.QUEUED.name)
            ps.setLong(5, System.currentTimeMillis())
            ps.executeUpdate()
        }
        return true
    }

    @Synchronized
    override fun claimNext(nowEpochMs: Long, leaseMs: Long): TaskRecord? {
        var attempt = 0
        while (true) {
            try {
                return claimOnce(nowEpochMs, leaseMs)
            } catch (e: Exception) {
                val busy = e is org.sqlite.SQLiteException && (e.message ?: "").contains("BUSY", ignoreCase = true)
                if (!busy || attempt >= 20) throw e
                attempt++
                Thread.sleep(25L * attempt)
            }
        }
    }

    private fun claimOnce(nowEpochMs: Long, leaseMs: Long): TaskRecord? {
        connection.autoCommit = false
        try {
            // Single atomic UPDATE: selection and state transition happen in one statement,
            // so two processes cannot claim the same task between a SELECT and an UPDATE.
            val sql = "UPDATE tasks SET state = ?, attempts = attempts + 1, lease_until = ?, updated_at = ? " +
                "WHERE task_id = (" +
                "SELECT task_id FROM tasks WHERE state = ? OR (state = ? AND lease_until IS NOT NULL AND lease_until < ?) " +
                "ORDER BY updated_at, task_id LIMIT 1) " +
                "AND (state = ? OR (state = ? AND lease_until IS NOT NULL AND lease_until < ?)) " +
                "RETURNING task_id"
            val claimedId = connection.prepareStatement(sql).use { ps ->
                ps.setString(1, TaskState.RUNNING.name)
                ps.setLong(2, nowEpochMs + leaseMs)
                ps.setLong(3, nowEpochMs)
                ps.setString(4, TaskState.QUEUED.name)
                ps.setString(5, TaskState.RUNNING.name)
                ps.setLong(6, nowEpochMs)
                ps.setString(7, TaskState.QUEUED.name)
                ps.setString(8, TaskState.RUNNING.name)
                ps.setLong(9, nowEpochMs)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
            val record = claimedId?.let { find(it) }
            connection.commit()
            return record
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    @Synchronized
    override fun complete(taskId: String, nowEpochMs: Long) {
        connection.prepareStatement(
            "UPDATE tasks SET state = ?, lease_until = NULL, last_error = NULL, updated_at = ? WHERE task_id = ?",
        ).use { ps ->
            ps.setString(1, TaskState.SUCCEEDED.name)
            ps.setLong(2, nowEpochMs)
            ps.setString(3, taskId)
            ps.executeUpdate()
        }
    }

    @Synchronized
    override fun fail(taskId: String, error: String, nowEpochMs: Long, maxAttempts: Int): TaskState {
        val current = find(taskId) ?: return TaskState.FAILED
        val next = if (current.attempts >= maxAttempts) TaskState.FAILED else TaskState.QUEUED
        connection.prepareStatement(
            "UPDATE tasks SET state = ?, lease_until = NULL, last_error = ?, updated_at = ? WHERE task_id = ?",
        ).use { ps ->
            ps.setString(1, next.name)
            ps.setString(2, error)
            ps.setLong(3, nowEpochMs)
            ps.setString(4, taskId)
            ps.executeUpdate()
        }
        return next
    }

    @Synchronized
    override fun cancel(taskId: String): Boolean {
        connection.prepareStatement(
            "UPDATE tasks SET state = ?, lease_until = NULL, updated_at = ? WHERE task_id = ? AND state = ?",
        ).use { ps ->
            ps.setString(1, TaskState.CANCELLED.name)
            ps.setLong(2, System.currentTimeMillis())
            ps.setString(3, taskId)
            ps.setString(4, TaskState.QUEUED.name)
            return ps.executeUpdate() > 0
        }
    }

    @Synchronized
    override fun recoverExpiredLeases(nowEpochMs: Long): Int {
        connection.prepareStatement(
            "UPDATE tasks SET state = ?, lease_until = NULL, updated_at = ? WHERE state = ? AND lease_until IS NOT NULL AND lease_until < ?",
        ).use { ps ->
            ps.setString(1, TaskState.QUEUED.name)
            ps.setLong(2, nowEpochMs)
            ps.setString(3, TaskState.RUNNING.name)
            ps.setLong(4, nowEpochMs)
            return ps.executeUpdate()
        }
    }

    @Synchronized
    override fun find(taskId: String): TaskRecord? =
        connection.prepareStatement("SELECT * FROM tasks WHERE task_id = ?").use { ps ->
            ps.setString(1, taskId)
            ps.executeQuery().use { rs -> if (rs.next()) toRecord(rs) else null }
        }

    @Synchronized
    override fun pendingCount(): Int =
        connection.prepareStatement("SELECT COUNT(*) FROM tasks WHERE state IN (?, ?)").use { ps ->
            ps.setString(1, TaskState.QUEUED.name)
            ps.setString(2, TaskState.RUNNING.name)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

    private fun toRecord(rs: java.sql.ResultSet): TaskRecord {
        val lease = rs.getLong("lease_until")
        val leaseIsNull = rs.wasNull()
        return TaskRecord(
            taskId = rs.getString("task_id"),
            type = rs.getString("type"),
            payload = rs.getString("payload"),
            state = TaskState.valueOf(rs.getString("state")),
            attempts = rs.getInt("attempts"),
            leaseUntilEpochMs = if (leaseIsNull) null else lease,
            lastError = rs.getString("last_error"),
        )
    }

    override fun close() {
        connection.close()
    }
}
