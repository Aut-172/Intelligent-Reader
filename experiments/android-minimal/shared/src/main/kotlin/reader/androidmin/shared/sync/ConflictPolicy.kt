package reader.androidmin.shared.sync

/**
 * 同步冲突策略（D-13 候选 A：笔记保留冲突副本，进度最新有效记录胜出，删除用 tombstone）。
 * 仅表达规则本身，不声明已被项目负责人批准。
 */

enum class ItemKind { NOTE, PROGRESS }

/** 冲突双方各自的变化。 */
sealed class SideChange {
    data class Upsert(val revision: Revision) : SideChange()
    object Delete : SideChange()
}

/** 冲突处理结果：本地最终保留什么，以及是否生成冲突副本。 */
sealed class ConflictResolution {
    data class KeepLocal(val keepConflictCopy: Boolean) : ConflictResolution()
    object ApplyRemote : ConflictResolution()
    object Tombstone : ConflictResolution()
}

object ConflictPolicy {

    fun resolve(kind: ItemKind, local: SideChange, remote: SideChange): ConflictResolution {
        // 删除与编辑竞争：删除胜出（tombstone），避免已删除内容被离线设备复活。
        if (local == SideChange.Delete || remote == SideChange.Delete) {
            return ConflictResolution.Tombstone
        }

        val localRev = (local as SideChange.Upsert).revision
        val remoteRev = (remote as SideChange.Upsert).revision

        return when (kind) {
            // 笔记：关键内容不静默丢失，保留本地并生成冲突副本。
            ItemKind.NOTE -> ConflictResolution.KeepLocal(keepConflictCopy = true)
            // 进度：最新有效记录胜出；版本相同时保留本地（确定性 tie-break）。
            ItemKind.PROGRESS ->
                if (localRev.value >= remoteRev.value) ConflictResolution.KeepLocal(keepConflictCopy = false)
                else ConflictResolution.ApplyRemote
        }
    }
}
