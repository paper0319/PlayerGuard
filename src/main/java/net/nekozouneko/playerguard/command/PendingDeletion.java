package net.nekozouneko.playerguard.command;

import java.util.UUID;

/** Immutable command-delete confirmation state, scoped to its initiator. */
public record PendingDeletion(String protectionId, UUID targetOwnerUuid, UUID initiatorUuid,
                              boolean adminOperation, long expiresAt) {
    public PendingDeletion ifValidAt(long now) {
        return now <= expiresAt ? this : null;
    }
}
