package net.nekozouneko.playerguard.command;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PendingDeletionTest {
    @Test
    void retainsAllAdministratorDeletionConfirmationFieldsUntilExpiry() {
        UUID actor = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        PendingDeletion deletion = new PendingDeletion("48c2ca4", owner, actor, true, 200L);

        assertEquals("48c2ca4", deletion.protectionId());
        assertEquals(owner, deletion.targetOwnerUuid());
        assertEquals(actor, deletion.initiatorUuid());
        assertEquals(true, deletion.adminOperation());
        assertEquals(deletion, deletion.ifValidAt(200L));
        assertNull(deletion.ifValidAt(201L));
    }
}
