package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public final class ProtectionTeleportRepository {
    private final File file;
    private final YamlConfiguration yaml;

    public ProtectionTeleportRepository(PlayerGuard plugin) {
        this.file = new File(plugin.getDataFolder(), "teleports.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public TeleportPoint find(ProtectedRegion region) {
        String path = region.getId();
        if (!yaml.contains(path + ".world")) return null;
        try {
            return new TeleportPoint(
                    region.getId(),
                    yaml.getString(path + ".world"),
                    yaml.getDouble(path + ".x"),
                    yaml.getDouble(path + ".y"),
                    yaml.getDouble(path + ".z"),
                    (float) yaml.getDouble(path + ".yaw"),
                    (float) yaml.getDouble(path + ".pitch"),
                    UUID.fromString(yaml.getString(path + ".setter")),
                    Instant.parse(yaml.getString(path + ".createdAt"))
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public void save(ProtectedRegion region, Location location, UUID setter) {
        String path = region.getId();
        yaml.set(path + ".regionId", region.getId());
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".worldUuid", location.getWorld().getUID().toString());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", location.getYaw());
        yaml.set(path + ".pitch", location.getPitch());
        yaml.set(path + ".setter", setter.toString());
        yaml.set(path + ".createdAt", Instant.now().toString());
        flush();
    }

    public void delete(ProtectedRegion region) {
        yaml.set(region.getId(), null);
        flush();
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save protection teleports", ex);
        }
    }

    public record TeleportPoint(String regionId, String worldName, double x, double y, double z, float yaw, float pitch, UUID setter, Instant createdAt) {
        public Location location() {
            World world = Bukkit.getWorld(worldName);
            return world == null ? null : new Location(world, x, y, z, yaw, pitch);
        }
    }
}
