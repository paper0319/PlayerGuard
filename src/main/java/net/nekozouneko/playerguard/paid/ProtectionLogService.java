package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ProtectionLogService {
    public void log(ProtectedRegion region, String type, UUID actor, String target, String before, String after, String detail) {
        if (region == null || !PGConfig.isProtectionLogEnabled()) return;
        List<ProtectionLogEntry> entries = readEntries(region, false);
        entries.add(new ProtectionLogEntry(System.currentTimeMillis(), type, actor, target, before, after, region.getId(), detail));
        int max = PGConfig.getProtectionLogMaxEntries();
        while (entries.size() > max) entries.remove(0);
        save(region, entries);
    }

    public List<ProtectionLogEntry> getEntries(ProtectedRegion region, boolean newestFirst) {
        return readEntries(region, newestFirst);
    }

    private List<ProtectionLogEntry> readEntries(ProtectedRegion region, boolean newestFirst) {
        String raw = region.getFlag(PGCustomFlags.PROTECTION_LOGS);
        List<ProtectionLogEntry> entries = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\n")) {
                ProtectionLogEntry entry = ProtectionLogEntry.decode(line);
                if (entry != null) entries.add(entry);
            }
        }
        if (newestFirst) Collections.reverse(entries);
        return entries;
    }

    private void save(ProtectedRegion region, List<ProtectionLogEntry> entries) {
        StringBuilder out = new StringBuilder();
        for (ProtectionLogEntry entry : entries) {
            if (out.length() > 0) out.append('\n');
            out.append(entry.encode());
        }
        region.setFlag(PGCustomFlags.PROTECTION_LOGS, out.toString());
    }
}