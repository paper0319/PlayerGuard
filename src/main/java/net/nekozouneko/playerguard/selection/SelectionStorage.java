package net.nekozouneko.playerguard.selection;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.regions.CuboidRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionStorage {
    private final Map<UUID, CuboidRegion> selection = new ConcurrentHashMap<>();
    private final Map<UUID, String> worlds = new ConcurrentHashMap<>();

    public void clear() {
        selection.clear();
        worlds.clear();
    }

    public void clear(UUID uuid) {
        selection.remove(uuid);
        worlds.remove(uuid);
    }

    public void putSelection(UUID uuid, CuboidRegion cr) {
        putSelection(uuid, cr, null);
    }

    public void putSelection(UUID uuid, CuboidRegion cr, String worldName) {
        Preconditions.checkArgument(cr != null);
        selection.put(uuid, cr.clone());
        if (worldName != null) worlds.put(uuid, worldName);
    }

    public CuboidRegion getSelection(UUID uuid) {
        return selection.get(uuid);
    }

    public String getWorldName(UUID uuid) {
        return worlds.get(uuid);
    }

    public Map<UUID, CuboidRegion> getSelections() {
        return new HashMap<>(selection);
    }
}