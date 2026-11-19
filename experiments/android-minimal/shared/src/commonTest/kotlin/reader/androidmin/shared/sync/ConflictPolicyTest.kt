package reader.androidmin.shared.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class ConflictPolicyTest {

    @Test
    fun `笔记冲突保留本地并生成冲突副本`() {
        val r = ConflictPolicy.resolve(
            ItemKind.NOTE,
            SideChange.Upsert(Revision(2)),
            SideChange.Upsert(Revision(3)),
        )
        assertEquals(ConflictResolution.KeepLocal(true), r)
    }

    @Test
    fun `进度冲突 - 高版本胜出 同版本保留本地`() {
        assertEquals(
            ConflictResolution.ApplyRemote,
            ConflictPolicy.resolve(ItemKind.PROGRESS, SideChange.Upsert(Revision(2)), SideChange.Upsert(Revision(3))),
        )
        assertEquals(
            ConflictResolution.KeepLocal(false),
            ConflictPolicy.resolve(ItemKind.PROGRESS, SideChange.Upsert(Revision(3)), SideChange.Upsert(Revision(2))),
        )
        assertEquals(
            ConflictResolution.KeepLocal(false),
            ConflictPolicy.resolve(ItemKind.PROGRESS, SideChange.Upsert(Revision(3)), SideChange.Upsert(Revision(3))),
        )
    }

    @Test
    fun `删除与编辑竞争 - 删除胜出 tombstone`() {
        assertEquals(
            ConflictResolution.Tombstone,
            ConflictPolicy.resolve(ItemKind.NOTE, SideChange.Delete, SideChange.Upsert(Revision(3))),
        )
        assertEquals(
            ConflictResolution.Tombstone,
            ConflictPolicy.resolve(ItemKind.PROGRESS, SideChange.Upsert(Revision(3)), SideChange.Delete),
        )
    }
}
