package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Persists payment metadata on the WorldGuard region; legacy seven-field values remain readable. */
public final class ProtectionPaymentRepository {
    public void save(ProtectedRegion region, ProtectionPaymentRecord record) {
        region.setFlag(PGCustomFlags.PAYMENT_RECORD, String.join("|", record.owner().toString(), record.protectionId(), Long.toString(record.volume()), Long.toString(record.paidVolume()), record.amountPaid().toPlainString(), Long.toString(record.createdAt().toEpochMilli()), Boolean.toString(record.refunded()), record.payer().toString()));
    }
    public ProtectionPaymentRecord find(ProtectedRegion region) {
        String value = region.getFlag(PGCustomFlags.PAYMENT_RECORD);
        if (value == null || value.isBlank()) return null;
        try {
            String[] values = value.split("\\|", -1);
            if (values.length != 7 && values.length != 8) return null;
            UUID owner = UUID.fromString(values[0]);
            UUID payer = values.length == 8 && !values[7].isBlank() ? UUID.fromString(values[7]) : owner;
            return new ProtectionPaymentRecord(owner, values[1], Long.parseLong(values[2]), Long.parseLong(values[3]), new BigDecimal(values[4]), Instant.ofEpochMilli(Long.parseLong(values[5])), Boolean.parseBoolean(values[6]), payer);
        } catch (RuntimeException ex) { return null; }
    }
}