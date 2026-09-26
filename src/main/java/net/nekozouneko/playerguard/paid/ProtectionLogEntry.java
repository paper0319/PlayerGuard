package net.nekozouneko.playerguard.paid;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ProtectionLogEntry(long at, String type, UUID actor, String target, String before,
                                 String after, String regionId, String detail) {
    public String encode() {
        return at + "|" + enc(type) + "|" + (actor == null ? "" : actor) + "|" + enc(target) + "|" + enc(before)
                + "|" + enc(after) + "|" + enc(regionId) + "|" + enc(detail);
    }

    public static ProtectionLogEntry decode(String raw) {
        try {
            String[] v = raw.split("\\|", -1);
            if (v.length != 8) return null;
            UUID actor = v[2].isBlank() ? null : UUID.fromString(v[2]);
            return new ProtectionLogEntry(Long.parseLong(v[0]), dec(v[1]), actor, dec(v[3]), dec(v[4]), dec(v[5]), dec(v[6]), dec(v[7]));
        } catch (RuntimeException ex) { return null; }
    }

    public Instant instant() { return Instant.ofEpochMilli(at); }
    private static String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(value == null ? "" : value).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }
}