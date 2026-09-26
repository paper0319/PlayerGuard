package net.nekozouneko.playerguard.paid;

import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CreationConfirmationSettings {
    private final PlayerGuard plugin;
    private final File file;
    private final Map<UUID, CreationConfirmationMode> modes = new ConcurrentHashMap<>();

    public CreationConfirmationSettings(PlayerGuard plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "creation-confirmation.yml");
    }

    public void load() {
        modes.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                modes.put(uuid, CreationConfirmationMode.parse(yaml.getString(key + ".confirmationMode")));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid creation confirmation player UUID in " + file.getName() + ": " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, CreationConfirmationMode> entry : modes.entrySet()) {
            yaml.set(entry.getKey() + ".confirmationMode", entry.getValue().name());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save creation confirmation settings: " + ex.getMessage());
        }
    }

    public CreationConfirmationMode get(UUID uuid) {
        return modes.getOrDefault(uuid, PGConfig.getDefaultCreationConfirmationMode());
    }

    public CreationConfirmationMode toggle(UUID uuid) {
        CreationConfirmationMode next = get(uuid).next();
        modes.put(uuid, next);
        save();
        return next;
    }
}
