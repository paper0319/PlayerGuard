package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

public final class ProtectionNameRepository {
    public String displayName(ProtectedRegion region) {
        String name = region.getFlag(PGCustomFlags.DISPLAY_NAME);
        return name == null || name.isBlank() ? region.getId() : name;
    }

    public ValidationResult validate(String input) {
        if (input == null) return ValidationResult.INVALID_LENGTH;
        String name = input.trim();
        if (name.length() < 1 || name.length() > 32) return ValidationResult.INVALID_LENGTH;
        if (name.contains("\n") || name.contains("\r")) return ValidationResult.LINE_BREAK;
        if (name.indexOf('\u00a7') >= 0 || name.indexOf('&') >= 0) return ValidationResult.COLOR_CODE;
        return ValidationResult.OK;
    }

    public void save(ProtectedRegion region, String input) {
        region.setFlag(PGCustomFlags.DISPLAY_NAME, input.trim());
    }

    public enum ValidationResult {
        OK,
        INVALID_LENGTH,
        COLOR_CODE,
        LINE_BREAK
    }
}