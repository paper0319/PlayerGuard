package net.nekozouneko.playerguard.gui;

import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ChatInputManager implements Listener {
    private static final Map<UUID, BiConsumer<Player, String>> WAITING = new ConcurrentHashMap<>();

    public static void waitFor(Player player, BiConsumer<Player, String> consumer) {
        WAITING.put(player.getUniqueId(), consumer);
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "チャットに入力してください。キャンセルするには cancel と入力してください。");
    }

    public static void clear() {
        WAITING.clear();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.isRuntimeReady()) return;
        BiConsumer<Player, String> consumer = WAITING.remove(event.getPlayer().getUniqueId());
        if (consumer == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getScheduler().runOnEntity(event.getPlayer(), () -> consumer.accept(event.getPlayer(), message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        WAITING.remove(event.getPlayer().getUniqueId());
    }
}