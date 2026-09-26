package net.nekozouneko.playerguard.paid;

import com.sk89q.worldedit.regions.CuboidRegion;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.ProtectionCreationService;
import net.nekozouneko.playerguard.scheduler.PGTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Confirmation state belongs to the player, not to a particular Inventory view. */
public final class CreationConfirmationManager implements Listener {
    private final PlayerGuard plugin;
    private final Map<UUID, PendingProtectionCreation> pending = new ConcurrentHashMap<>();
    public CreationConfirmationManager(PlayerGuard plugin) { this.plugin = plugin; }
    public void begin(Player player, CuboidRegion selection, World world, ProtectionPricingPreview.Result preview) {
        clear(player.getUniqueId());
        Instant now = Instant.now();
        PendingProtectionCreation state = new PendingProtectionCreation(player.getUniqueId(), world.getUID(), world.getName(), selection.clone(), preview, plugin.getCreationConfirmationSettings().get(player.getUniqueId()), now, now.plusSeconds(PGConfig.getChatConfirmationTimeoutSeconds()));
        pending.put(player.getUniqueId(), state);
        state.timeoutTask = plugin.getScheduler().runTimer(() -> expireIfNeeded(player.getUniqueId()), 20L, 20L);
        if (state.confirmationMode == CreationConfirmationMode.CHAT) sendChatPreview(player, state);
    }
    public boolean hasPending(UUID uuid) { return pending.containsKey(uuid); }
    public PendingProtectionCreation getPending(UUID uuid) { return pending.get(uuid); }
    public boolean confirm(Player player) {
        PendingProtectionCreation state = pending.get(player.getUniqueId());
        if (state == null) { player.sendMessage(ChatColor.RED + "確認待ちの土地保護がありません。"); return true; }
        if (Instant.now().isAfter(state.expiresAt)) { expire(player.getUniqueId(), player); return true; }
        if (!state.beginProcessing()) { player.sendMessage(ChatColor.YELLOW + "土地保護を作成中です。しばらくお待ちください。"); return true; }
        World world = Bukkit.getWorld(state.worldUuid);
        if (world == null || !world.getName().equals(state.worldName)) { state.endProcessing(); clear(player.getUniqueId()); player.sendMessage(ChatColor.RED + "選択したワールドが見つかりません。"); return true; }
        ProtectionCreationService.create(player, state.selection.clone(), world, state.preview.total(), (success, message, latest) -> {
            if (!plugin.isRuntimeReady() || plugin.getScheduler() == null) return;
            plugin.getScheduler().runOnEntity(player, () -> {
                player.sendMessage(message);
                if (success) { clear(player.getUniqueId()); return; }
                state.endProcessing();
                if (latest != null && latest.total().compareTo(state.preview.total()) != 0) { state.preview = latest; player.sendMessage(ChatColor.YELLOW + "土地保護の料金が変更されました。"); player.sendMessage(ChatColor.GRAY + "最新の内容を確認してください。"); }
            });
        });
        return true;
    }
    public boolean cancel(Player player) {
        PendingProtectionCreation state = pending.get(player.getUniqueId());
        if (state != null && state.processing) { player.sendMessage(ChatColor.YELLOW + "土地保護を作成中のためキャンセルできません。"); return true; }
        boolean hadSelection = plugin.getSelectionStorage().getSelection(player.getUniqueId()) != null;
        if (state == null && !hadSelection) { player.sendMessage(ChatColor.RED + "キャンセルできる土地保護がありません。"); return true; }
        clear(player.getUniqueId()); plugin.getSelectionStorage().clear(player.getUniqueId()); player.sendMessage(ChatColor.RED + "土地保護の作成をキャンセルしました。"); return true;
    }
    public void clear(UUID uuid) { PendingProtectionCreation state = pending.remove(uuid); if (state != null && state.timeoutTask != null) state.timeoutTask.cancel(); }
    public void clearAll() { for (UUID uuid : pending.keySet()) clear(uuid); }
    public void cancelAllForReload() {
        for (UUID uuid : java.util.List.copyOf(pending.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            clear(uuid);
            plugin.getSelectionStorage().clear(uuid);
            if (player != null) player.sendMessage(ChatColor.YELLOW + "設定が再読み込みされたため、土地保護の確認を取り消しました。");
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer().getUniqueId()); }
    private void expireIfNeeded(UUID uuid) { PendingProtectionCreation state = pending.get(uuid); if (state != null && Instant.now().isAfter(state.expiresAt)) expire(uuid, Bukkit.getPlayer(uuid)); }
    private void expire(UUID uuid, Player player) { clear(uuid); if (player != null) { plugin.getSelectionStorage().clear(uuid); player.sendMessage(ChatColor.RED + "土地保護の確認時間が終了しました。"); player.sendMessage(ChatColor.GRAY + "もう一度、金の斧で範囲を選択してください。"); } }
    private void sendChatPreview(Player player, PendingProtectionCreation state) { player.sendMessage(ChatColor.AQUA + "土地保護の最終確認"); player.sendMessage(ChatColor.GRAY + "体積: " + ChatColor.YELLOW + state.volume + "ブロック"); player.sendMessage(ChatColor.GRAY + "必要金額: " + ChatColor.YELLOW + state.preview.total().toPlainString() + "円"); player.sendMessage(ChatColor.GREEN + "/claim または /hogo で作成"); player.sendMessage(ChatColor.RED + "/cancel でキャンセル"); }
    public static final class PendingProtectionCreation {
        public final UUID actorUuid, worldUuid; public final String worldName; public final CuboidRegion selection; public final long width, height, depth, volume, currentOwnedVolume, resultOwnedVolume, freeLimit, freeVolume, paidVolume; public final Instant createdAt, expiresAt; public final CreationConfirmationMode confirmationMode; public volatile ProtectionPricingPreview.Result preview; private volatile boolean processing; private PGTask timeoutTask;
        private PendingProtectionCreation(UUID actorUuid, UUID worldUuid, String worldName, CuboidRegion selection, ProtectionPricingPreview.Result preview, CreationConfirmationMode confirmationMode, Instant createdAt, Instant expiresAt) { this.actorUuid=actorUuid;this.worldUuid=worldUuid;this.worldName=worldName;this.selection=selection;this.preview=preview;this.confirmationMode=confirmationMode;this.createdAt=createdAt;this.expiresAt=expiresAt;width=Math.abs(selection.getMaximumPoint().x()-selection.getMinimumPoint().x())+1L;height=Math.abs(selection.getMaximumPoint().y()-selection.getMinimumPoint().y())+1L;depth=Math.abs(selection.getMaximumPoint().z()-selection.getMinimumPoint().z())+1L;volume=selection.getVolume();currentOwnedVolume=preview.beforeVolume();resultOwnedVolume=preview.afterVolume();freeLimit=preview.freeLimit();freeVolume=preview.freeAddedVolume();paidVolume=preview.paidAddedVolume(); }
        private synchronized boolean beginProcessing(){if(processing)return false;processing=true;return true;} private synchronized void endProcessing(){processing=false;}
    }
}