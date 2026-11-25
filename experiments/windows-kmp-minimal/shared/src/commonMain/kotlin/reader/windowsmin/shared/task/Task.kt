package reader.windowsmin.shared.task

/**
 * D-26 A client-side persistent task contract: durable queue with lease, retry,
 * cancellation and restart recovery. Storage is platform-specific (SQLite on Windows).
 */

enum class TaskState { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class TaskRecord(
    val taskId: String,
    val type: String,
    val payload: String,
    val state: TaskState,
    val attempts: Int,
    val leaseUntilEpochMs: Long?,
    val lastError: String?,
)

interface TaskStore {
    /** Returns false when the same task id is already known (idempotent enqueue). */
    fun enqueue(taskId: String, type: String, payload: String): Boolean

    /** Claims one queued (or lease-expired) task and marks it RUNNING with a lease. */
    fun claimNext(nowEpochMs: Long, leaseMs: Long): TaskRecord?

    fun complete(taskId: String, nowEpochMs: Long)

    /** Returns the resulting state: QUEUED (retry) or FAILED after maxAttempts. */
    fun fail(taskId: String, error: String, nowEpochMs: Long, maxAttempts: Int): TaskState

    fun cancel(taskId: String): Boolean

    /** Moves RUNNING tasks whose lease expired back to QUEUED; returns affected count. */
    fun recoverExpiredLeases(nowEpochMs: Long): Int

    fun find(taskId: String): TaskRecord?

    fun pendingCount(): Int
}
