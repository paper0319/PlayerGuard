package net.nekozouneko.playerguard.paid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProtectionPaymentRecord(UUID owner, String protectionId, long volume, long paidVolume,
                                      BigDecimal amountPaid, Instant createdAt, boolean refunded) {
    public ProtectionPaymentRecord refund() { return new ProtectionPaymentRecord(owner, protectionId, volume, paidVolume, amountPaid, createdAt, true); }
}
