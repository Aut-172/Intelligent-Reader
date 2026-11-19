package reader.windowsmin.shared.sync

/**
 * Conflict policy (D-13 A): notes keep a conflict copy, progress keeps the newest valid
 * record, delete wins as a tombstone.
 */

enum class ItemKind { NOTE, PROGRESS }

sealed class SideChange {
    data class Upsert(val revision: Revision) : SideChange()
    object Delete : SideChange()
}

sealed class ConflictResolution {
    data class KeepLocal(val keepConflictCopy: Boolean) : ConflictResolution()
    object ApplyRemote : ConflictResolution()
    object Tombstone : ConflictResolution()
}

object ConflictPolicy {

    fun resolve(kind: ItemKind, local: SideChange, remote: SideChange): ConflictResolution {
        if (local == SideChange.Delete || remote == SideChange.Delete) {
            return ConflictResolution.Tombstone
        }

        val localRev = (local as SideChange.Upsert).revision
        val remoteRev = (remote as SideChange.Upsert).revision

        return when (kind) {
            ItemKind.NOTE -> ConflictResolution.KeepLocal(keepConflictCopy = true)
            ItemKind.PROGRESS ->
                if (localRev.value >= remoteRev.value) ConflictResolution.KeepLocal(keepConflictCopy = false)
                else ConflictResolution.ApplyRemote
        }
    }
}
