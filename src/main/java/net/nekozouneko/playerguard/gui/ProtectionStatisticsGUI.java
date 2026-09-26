package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

public final class ProtectionStatisticsGUI extends AbstractGUI {
    public static final int SIZE = 27;
    private static final int SLOT_BASIC = 10, SLOT_FREE = 12, SLOT_PAID = 13, SLOT_BREAKDOWN = 14, SLOT_MEMBERS = 16, SLOT_BACK = 26;
    public ProtectionStatisticsGUI(Player player, AbstractGUI parent) { super(player, parent); }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "\u571f\u5730\u4fdd\u8b77\u7d71\u8a08");
        inventory.clear();
        ItemStack pane = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, pane);
        long owned = 0, sub = 0, volume = 0, memberTotal = 0, subownerTotal = 0, max = 0, min = Long.MAX_VALUE;
        BigDecimal paid = BigDecimal.ZERO, refund = BigDecimal.ZERO;
        Instant oldest = null, newest = null;
        for (Map.Entry<ProtectedRegion, World> entry : PGUtil.getPlayerRegions(getPlayer()).entrySet()) {
            ProtectedRegion region = entry.getKey();
            RegionRoles.Role role = RegionRoles.roleOf(region, getPlayer().getUniqueId());
            if (role == RegionRoles.Role.PRIMARY_OWNER) {
                owned++; volume += region.volume(); max = Math.max(max, region.volume()); min = Math.min(min, region.volume());
                memberTotal += region.getMembers().getUniqueIds().size();
                subownerTotal += Math.max(0, region.getOwners().getUniqueIds().size() - 1);
                ProtectionPaymentRecord record = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
                if (record != null) { paid = paid.add(record.amountPaid()); refund = refund.add(ProtectionGuiText.refund(record.amountPaid(), record.createdAt(), Instant.now())); oldest = oldest == null || record.createdAt().isBefore(oldest) ? record.createdAt() : oldest; newest = newest == null || record.createdAt().isAfter(newest) ? record.createdAt() : newest; }
            } else if (role == RegionRoles.Role.SUB_OWNER) sub++;
        }
        long freeLimit = PlayerGuard.getInstance().getProtectLimit(getPlayer());
        long freeUsed = Math.max(0, Math.min(volume, freeLimit));
        long freeRemaining = Math.max(0, freeLimit - freeUsed);
        long paidVolume = Math.max(0, volume - freeUsed);
        BigDecimal usage = freeLimit <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(freeUsed * 100D / freeLimit).setScale(1, RoundingMode.HALF_UP);
        String rate = PlayerGuard.getInstance().getProgressivePricingService().currentRate(volume, freeLimit).stripTrailingZeros().toPlainString();
        inventory.setItem(SLOT_BASIC, item(Material.NETHER_STAR, ChatColor.AQUA + "\u00a7l\u57fa\u672c\u60c5\u5831", "\u00a77\u73fe\u5728\u306e\u4fdd\u8b77\u6570: \u00a7f" + number(owned + sub), "\u00a77\u81ea\u5206\u304c\u30aa\u30fc\u30ca\u30fc\u306e\u4fdd\u8b77\u6570: \u00a7f" + number(owned), "\u00a77\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc\u306e\u4fdd\u8b77\u6570: \u00a7f" + number(sub)));
        inventory.setItem(SLOT_FREE, item(Material.EMERALD, ChatColor.GREEN + "\u00a7l\u7121\u6599\u4fdd\u8b77\u60c5\u5831", "\u00a77\u7121\u6599\u4fdd\u8b77\u4e0a\u9650: \u00a7f" + blocks(freeLimit), "\u00a77\u7121\u6599\u4fdd\u8b77\u4f7f\u7528\u91cf: \u00a7e" + blocks(freeUsed), "\u00a77\u6b8b\u308a\u7121\u6599\u4f53\u7a4d: \u00a7a" + blocks(freeRemaining), "\u00a77\u7121\u6599\u67a0\u4f7f\u7528\u7387: \u00a7f" + usage + "%"));
        inventory.setItem(SLOT_PAID, item(Material.GOLD_INGOT, ChatColor.GOLD + "\u00a7l\u6709\u6599\u4fdd\u8b77\u60c5\u5831", "\u00a77\u6709\u6599\u4fdd\u8b77\u4f53\u7a4d: \u00a7f" + blocks(paidVolume), "\u00a77\u7d2f\u8a08\u652f\u6255\u984d: \u00a7e" + money(paid), "\u00a77\u8fd4\u91d1\u4e88\u5b9a\u7dcf\u984d: \u00a7a" + money(refund), "\u00a77\u73fe\u5728\u306e\u4fa1\u683c\u5e2f: \u00a7e" + rate + "\u5186/\u30d6\u30ed\u30c3\u30af"));
        inventory.setItem(SLOT_BREAKDOWN, item(Material.CHEST, ChatColor.YELLOW + "\u00a7l\u4fdd\u8b77\u5185\u8a33", "\u00a77\u5408\u8a08\u4fdd\u8b77\u4f53\u7a4d: \u00a7f" + blocks(volume), "\u00a77\u6700\u5927\u306e\u4fdd\u8b77: \u00a7f" + blocks(max), "\u00a77\u6700\u5c0f\u306e\u4fdd\u8b77: \u00a7f" + blocks(min == Long.MAX_VALUE ? 0 : min), "\u00a77\u6700\u3082\u53e4\u3044\u4fdd\u8b77: \u00a7f" + ProtectionGuiText.date(oldest), "\u00a77\u6700\u3082\u65b0\u3057\u3044\u4fdd\u8b77: \u00a7f" + ProtectionGuiText.date(newest)));
        inventory.setItem(SLOT_MEMBERS, item(Material.PLAYER_HEAD, ChatColor.LIGHT_PURPLE + "\u00a7l\u30e1\u30f3\u30d0\u30fc\u60c5\u5831", "\u00a77\u5ef6\u3079\u30e1\u30f3\u30d0\u30fc\u6570: \u00a7f" + number(memberTotal) + "\u4eba", "\u00a77\u5ef6\u3079\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc\u6570: \u00a7f" + number(subownerTotal) + "\u4eba"));
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "\u623b\u308b").build());
    }
    private static ItemStack item(Material material, String name, String... lines) { String[] lore = new String[lines.length + 1]; lore[0] = ChatColor.DARK_GRAY + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"; System.arraycopy(lines, 0, lore, 1, lines.length); return ItemStackBuilder.of(material).name(name).lore(lore).build(); }
    private static String number(long value) { return String.format("%,d", value); }
    private static String blocks(long value) { return number(value) + "\u30d6\u30ed\u30c3\u30af"; }
    private static String money(BigDecimal value) { return String.format("%,d", value.setScale(0, RoundingMode.HALF_UP).longValue()) + "\u5186"; }
    @EventHandler public void onClick(InventoryClickEvent event) { if (event.getView().getTopInventory().getHolder() == this) { event.setCancelled(true); if (event.getRawSlot() == SLOT_BACK) back(); } }
}