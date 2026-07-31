package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Persists payment metadata on the WorldGuard region itself. */
public final class ProtectionPaymentRepository {
    public void save(ProtectedRegion region, ProtectionPaymentRecord record) {
        region.setFlag(PGCustomFlags.PAYMENT_RECORD, String.join("|", record.owner().toString(), record.protectionId(),
                Long.toString(record.volume()), Long.toString(record.paidVolume()), record.amountPaid().toPlainString(),
                Long.toString(record.createdAt().toEpochMilli()), Boolean.toString(record.refunded())));
    }
    public ProtectionPaymentRecord find(ProtectedRegion region) {
        String value = region.getFlag(PGCustomFlags.PAYMENT_RECORD);
        if (value == null || value.isBlank()) return null;
        try {
            String[] values = value.split("\\|", -1);
            if (values.length != 7) return null;
            return new ProtectionPaymentRecord(UUID.fromString(values[0]), values[1], Long.parseLong(values[2]), Long.parseLong(values[3]), new BigDecimal(values[4]), Instant.ofEpochMilli(Long.parseLong(values[5])), Boolean.parseBoolean(values[6]));
        } catch (RuntimeException ex) { return null; }
    }
}
