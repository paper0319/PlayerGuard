package net.nekozouneko.playerguard.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.GuardFlags;
import net.nekozouneko.playerguard.gui.ProtectionGuiText;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
import net.nekozouneko.playerguard.paid.ProtectionPricingPreview;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class ProtectionCreationService {
    public interface Callback { void done(boolean success, String message, ProtectionPricingPreview.Result latestPreview); }

    private ProtectionCreationService() {}

    public static ProtectionPricingPreview.Result preview(Player player, CuboidRegion selection) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        long used = plugin.getProtectionUsed(player);
        long limit = plugin.getProtectLimit(player);
        return ProtectionPricingPreview.calculate(used, selection.getVolume(), limit, plugin.getPaidExtensionConfig().rates());
    }

    public static void create(Player player, CuboidRegion selection, World world, BigDecimal expectedCharge, Callback callback) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        plugin.getScheduler().runGlobal(() -> {
            if (!player.isOnline()) { callback.done(false, ChatColor.RED + "プレイヤーがオンラインではありません。", null); return; }
            if (selection == null || world == null || !player.getWorld().equals(world)) { callback.done(false, ChatColor.RED + "選択したワールドが現在のワールドと一致しません。", null); return; }
            if (selection.getVolume() <= 0) { callback.done(false, ChatColor.RED + "選択範囲の体積が不正です。", null); return; }
            if (36 > selection.getVolume()) { callback.done(false, PGMessages.error("土地保護の最小サイズは36ブロックです。現在のサイズ: %s", PGMessages.highlight(selection.getVolume())), null); return; }

            ProtectionPricingPreview.Result latest = preview(player, selection);
            boolean adminFree = player.hasPermission("playerguard.admin.free-protection");
            if (!adminFree && expectedCharge != null && latest.total().compareTo(expectedCharge) != 0) {
                callback.done(false, ChatColor.YELLOW + "保護状況または残高が変化しました。最新の確認内容を表示します。", latest);
                return;
            }
            if (!plugin.getPaidExtensionConfig().enabled() && latest.afterVolume() > latest.freeLimit()) {
                callback.done(false, PGMessages.error("保護上限を超えています。使用量: %s / 追加分: %s / 上限: %s", PGMessages.highlight(latest.afterVolume()), PGMessages.highlight(selection.getVolume()), PGMessages.highlight(latest.freeLimit())), latest);
                return;
            }
            if (!adminFree && plugin.getPaidExtensionConfig().enabled() && latest.total().signum() > 0) {
                if (!plugin.getEconomyTransactionService().available()) { callback.done(false, ChatColor.RED + "経済プラグインが見つかりません。土地保護を作成できません。", latest); return; }
                if (BigDecimal.valueOf(plugin.getEconomyTransactionService().balance(player)).compareTo(latest.total()) < 0) { callback.done(false, ChatColor.RED + "所持金が不足しています。", latest); return; }
            }

            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = rc.get(BukkitAdapter.adapt(world));
            if (rm == null) { callback.done(false, ChatColor.RED + "WorldGuardの保護管理を取得できませんでした。", latest); return; }
            String id = generateId(rc);
            if (id == null) { callback.done(false, ChatColor.RED + "保護IDを生成できませんでした。", latest); return; }
            ProtectedRegion protect = new ProtectedCuboidRegion(id, selection.getPos1(), selection.getPos2());
            protect.getOwners().addPlayer(player.getUniqueId());
            RegionRoles.setPrimaryOwner(protect, player.getUniqueId());
            GuardFlags.initRegionFlags(protect);
            protect.setFlag(PlayerGuard.getGuardRegisteredFlag(), StateFlag.State.ALLOW);

            ApplicableRegionSet ars = rm.getApplicableRegions(protect);
            long overlap = ars.getRegions().stream().filter(pr -> !(pr instanceof GlobalProtectedRegion)).count();
            if (overlap > 0 || ars.testState(WorldGuardPlugin.inst().wrapPlayer(player), PlayerGuard.getGuardIgnoredFlag())) { callback.done(false, ChatColor.RED + "ほかの保護領域と重なっています。", latest); return; }
            long minDistance = Long.MAX_VALUE;
            for (ProtectedRegion other : rm.getRegions().values()) {
                if (other.getFlag(PlayerGuard.getGuardRegisteredFlag()) != StateFlag.State.ALLOW) continue;
                if (!PGConfig.doApplyToSamePlayerSRegion() && other.getOwners().contains(player.getUniqueId())) continue;
                long d = PGUtil.distanceBetweenRegions(protect, other);
                if (d >= 0) minDistance = Math.min(minDistance, d);
            }
            if (minDistance <= PGConfig.getMinSpacingBetweenRegions()) { callback.done(false, PGMessages.error("ほかの保護領域との距離が近すぎます。最短距離: %s", PGMessages.highlight(minDistance)), latest); return; }

            boolean withdrawn = false;
            if (!adminFree && latest.total().signum() > 0 && !plugin.getEconomyTransactionService().withdraw(player, latest.total())) { callback.done(false, ChatColor.RED + "支払いに失敗したため、土地保護を作成できませんでした。", latest); return; }
            withdrawn = !adminFree && latest.total().signum() > 0;
            try {
                rm.addRegion(protect);
                ProtectionPaymentRecord record = new ProtectionPaymentRecord(player.getUniqueId(), id, protect.volume(), latest.paidAddedVolume(), adminFree ? BigDecimal.ZERO : latest.total(), latest.total(), Instant.now(), false);
                plugin.getProtectionPaymentRepository().save(protect, record);
                plugin.getProtectionLogService().log(protect, "保護作成", player.getUniqueId(), null, null, Long.toString(protect.volume()), null);
                if (adminFree) plugin.getProtectionLogService().log(protect, "管理者による土地保護作成", player.getUniqueId(), null, latest.total().toPlainString(), "0", "管理者無料権限: 適用");
                if (!adminFree && latest.total().signum() > 0) plugin.getProtectionLogService().log(protect, "支払い", player.getUniqueId(), null, null, latest.total().toPlainString(), "paidVolume=" + record.paidVolume());
                plugin.getSelectionStorage().clear(player.getUniqueId());
            } catch (RuntimeException ex) {
                rm.removeRegion(id);
                if (withdrawn) plugin.getEconomyTransactionService().deposit(player, latest.total());
                callback.done(false, ChatColor.RED + "土地保護の保存に失敗したため、支払いを取り消しました。", latest);
                return;
            }
            callback.done(true,
                    ChatColor.GREEN + "土地保護を作成しました。\n"
                            + ChatColor.GRAY + "保護ID: " + ChatColor.WHITE + id + "\n"
                            + ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(protect.volume()) + "\n"
                            + (adminFree ? ChatColor.GRAY + "本来必要だった金額: " + ChatColor.YELLOW + ProtectionGuiText.money(latest.total()) + "\n" + ChatColor.GRAY + "実際の支払金額: " + ChatColor.GREEN + ProtectionGuiText.money(BigDecimal.ZERO) : ChatColor.GRAY + "支払金額: " + ChatColor.YELLOW + ProtectionGuiText.money(latest.total())),
                    latest);
        });
    }

    private static String generateId(RegionContainer rc) {
        Set<String> allRegionIds = new HashSet<>();
        rc.getLoaded().forEach(rm -> allRegionIds.addAll(rm.getRegions().keySet()));
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            String id = Integer.toHexString(random.nextInt(0x10000000));
            if (!allRegionIds.contains(id)) return id;
        }
        return null;
    }
}