package net.nekozouneko.playerguard.gui;

import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.PaidExtensionConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtectionGuideDetailGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_BACK = 18;
    private static final int SLOT_PREV = 21;
    private static final int SLOT_NEXT = 23;
    private int page;

    public ProtectionGuideDetailGUI(Player player, AbstractGUI parent, int page) {
        super(player, parent);
        this.page = page;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "ガイド詳細");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);
        page = Math.max(0, Math.min(page, ProtectionGuideTopic.values().length - 1));
        inventory.setItem(SLOT_INFO, item(ProtectionGuideTopic.values()[page]));
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
        if (page > 0) inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "前ページ").build());
        if (page < ProtectionGuideTopic.values().length - 1) inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "次ページ").build());
    }

    private ItemStack item(ProtectionGuideTopic topic) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "────────────");
        for (String line : topic.detail) lore.add(ChatColor.GRAY + line);
        addDynamicDetail(topic, lore);
        lore.add(ChatColor.DARK_GRAY + "────────────");
        return ItemStackBuilder.of(topic.material).name(topic.title).lore(lore.toArray(new String[0])).build();
    }

    private void addDynamicDetail(ProtectionGuideTopic topic, List<String> lore) {
        if (topic == ProtectionGuideTopic.RATES || topic == ProtectionGuideTopic.RATES) {
            lore.add(ChatColor.YELLOW + "現在の価格帯");
            java.util.NavigableMap<Long, BigDecimal> rates = PlayerGuard.getInstance().getPaidExtensionConfig().rates();
            for (Map.Entry<Long, BigDecimal> entry : rates.entrySet()) {
                Long next = rates.higherKey(entry.getKey());
                lore.add(ChatColor.GRAY + String.format("%,d%s: ", entry.getKey(), next == null ? "以上" : "～" + String.format("%,d", next)) + ChatColor.YELLOW + ProtectionGuiText.rate(entry.getValue()) + "円/ブロック");
            }
        }
        if (topic == ProtectionGuideTopic.REFUND) {
            PaidExtensionConfig c = PlayerGuard.getInstance().getPaidExtensionConfig();
            lore.add(ChatColor.YELLOW + "現在の返金率");
            lore.add("" + ChatColor.GRAY + c.refundPolicy().coolingOffMinutes() + "分以内: " + ChatColor.GREEN + ProtectionGuiText.percent(c.refundPolicy().coolingOffRate()));
            lore.add("" + ChatColor.GRAY + c.refundPolicy().coolingOffMinutes() + "分超～" + c.refundPolicy().normalPeriodDays() + "日以内: " + ChatColor.YELLOW + ProtectionGuiText.percent(c.refundPolicy().normalRefundRate()));
            lore.add("" + ChatColor.GRAY + c.refundPolicy().normalPeriodDays() + "日超: " + ChatColor.RED + ProtectionGuiText.percent(c.refundPolicy().longTermRefundRate()));
        }
        if (topic == ProtectionGuideTopic.TELEPORT) {
            lore.add(ChatColor.YELLOW + "現在の料金");
            lore.add(ChatColor.GRAY + "設定・変更: " + ChatColor.YELLOW + ProtectionGuiText.money(PGConfig.getTeleportSetupCost()));
            lore.add(ChatColor.GRAY + "テレポート: " + ChatColor.YELLOW + ProtectionGuiText.money(PGConfig.getTeleportCost()));
            lore.add(ChatColor.GRAY + "地点削除時の返金はありません。");
        }
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        switch (e.getRawSlot()) {
            case SLOT_BACK: back(); break;
            case SLOT_PREV: if (page > 0) { page--; init(); } break;
            case SLOT_NEXT: if (page < ProtectionGuideTopic.values().length - 1) { page++; init(); } break;
            default: break;
        }
    }
}
