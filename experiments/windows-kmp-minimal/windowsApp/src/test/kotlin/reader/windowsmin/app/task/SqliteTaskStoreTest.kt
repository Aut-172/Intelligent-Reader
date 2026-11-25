package reader.windowsmin.app.task

import reader.windowsmin.shared.task.TaskState
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteTaskStoreTest {

    private val dbFile: File = File.createTempFile("we07-task-", ".db").also { it.delete() }
    private val stores = mutableListOf<SqliteTaskStore>()

    private fun store(): SqliteTaskStore = SqliteTaskStore(dbFile).also { stores += it }

    @AfterTest
    fun cleanup() {
        stores.forEach { runCatching { it.close() } }
        dbFile.delete()
    }

    @Test
    fun enqueueIsIdempotent() {
        store().use { s ->
            assertTrue(s.enqueue("t1", "PARSE", "payload"))
            assertFalse(s.enqueue("t1", "PARSE", "payload-dup"))
            assertEquals(1, s.pendingCount())
            assertEquals("payload", s.find("t1")?.payload)
        }
    }

    @Test
    fun claimSetsRunningAndSecondClaimReturnsNull() {
        store().use { s ->
            s.enqueue("t1", "PARSE", "payload")
            val claimed = s.claimNext(1_000L, leaseMs = 10_000L)
            assertNotNull(claimed)
            assertEquals(TaskState.RUNNING, claimed.state)
            assertEquals(1, claimed.attempts)
            assertNull(s.claimNext(1_001L, leaseMs = 10_000L))
        }
    }

    @Test
    fun completeDrainsPending() {
        store().use { s ->
            s.enqueue("t1", "PARSE", "payload")
            s.claimNext(1_000L, 10_000L)
            s.complete("t1", 2_000L)
            assertEquals(TaskState.SUCCEEDED, s.find("t1")?.state)
            assertEquals(0, s.pendingCount())
        }
    }

    @Test
    fun expiredLeaseIsRecoveredAfterRestart() {
        store().use { s ->
            s.enqueue("t1", "PARSE", "payload")
            assertNotNull(s.claimNext(1_000L, leaseMs = 50L))
        }
        // Process "restarts": new store instance on the same database file.
        store().use { s2 ->
            val recovered = s2.recoverExpiredLeases(nowEpochMs = 2_000L)
            assertEquals(1, recovered)
            val reclaimed = s2.claimNext(2_000L, leaseMs = 10_000L)
            assertNotNull(reclaimed)
            assertEquals("t1", reclaimed.taskId)
            assertEquals(2, reclaimed.attempts)
        }
    }

    @Test
    fun failRetriesThenStopsAtMaxAttempts() {
        store().use { s ->
            s.enqueue("t1", "PARSE", "payload")
            s.claimNext(1_000L, 10_000L)
            assertEquals(TaskState.QUEUED, s.fail("t1", "transient", 1_100L, maxAttempts = 2))
            s.claimNext(1_200L, 10_000L)
            assertEquals(TaskState.FAILED, s.fail("t1", "fatal", 1_300L, maxAttempts = 2))
            assertEquals(0, s.pendingCount())
        }
    }

    @Test
    fun cancelRemovesTaskFromQueue() {
        store().use { s ->
            s.enqueue("t1", "PARSE", "payload")
            assertTrue(s.cancel("t1"))
            assertNull(s.claimNext(1_000L, 10_000L))
            assertEquals(TaskState.CANCELLED, s.find("t1")?.state)
        }
    }

    @Test
    fun queuedTasksSurviveRestart() {
        store().use { s -> s.enqueue("t1", "PARSE", "payload") }
        store().use { s2 ->
            assertEquals(1, s2.pendingCount())
            val claimed = s2.claimNext(1_000L, 10_000L)
            assertNotNull(claimed)
            assertEquals("payload", claimed.payload)
        }
    }
}
