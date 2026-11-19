package reader.windowsmin.shared.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class ConflictPolicyTest {

    @Test
    fun noteConflictKeepsLocalCopyWithConflictCopy() {
        val r = ConflictPolicy.resolve(
            ItemKind.NOTE,
            SideChange.Upsert(Revision(2)),
            SideChange.Upsert(Revision(3)),
        )
        assertEquals(ConflictResolution.KeepLocal(true), r)
    }

    @Test
    fun progressConflictKeepsNewestAndTieKeepsLocal() {
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
    fun deleteWinsOverEditAsTombstone() {
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
