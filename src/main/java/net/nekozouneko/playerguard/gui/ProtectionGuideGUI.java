package net.nekozouneko.playerguard.gui;

import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ProtectionGuideGUI extends AbstractGUI {
    static final int SIZE = 54;

    static final int SLOT_ABOUT = 11;
    static final int SLOT_AXE_USAGE = 12;
    static final int SLOT_RANGE = 13;
    static final int SLOT_CREATE = 14;
    static final int SLOT_CONFIRMATION = 15;

    static final int SLOT_FREE_LIMIT = 20;
    static final int SLOT_PAYMENT = 21;
    static final int SLOT_RATES = 22;
    static final int SLOT_REFUND = 23;

    static final int SLOT_MEMBERS = 27;
    static final int SLOT_SUBOWNERS = 28;
    static final int SLOT_PERMISSIONS = 29;
    static final int SLOT_FLAGS = 30;
    static final int SLOT_CATEGORY = 32;
    static final int SLOT_TELEPORT = 33;
    static final int SLOT_LOGS = 34;
    static final int SLOT_DELETE = 35;

    static final int SLOT_FAQ = 39;
    static final int SLOT_GET_AXE = 41;
    static final int SLOT_BACK = 53;

    static final int[] TOPIC_SLOTS = {
            SLOT_ABOUT, SLOT_AXE_USAGE, SLOT_RANGE, SLOT_CREATE, SLOT_CONFIRMATION,
            SLOT_FREE_LIMIT, SLOT_PAYMENT, SLOT_RATES, SLOT_REFUND,
            SLOT_MEMBERS, SLOT_SUBOWNERS, SLOT_PERMISSIONS, SLOT_FLAGS,
            SLOT_CATEGORY, SLOT_TELEPORT, SLOT_LOGS, SLOT_DELETE, SLOT_FAQ
    };

    private final Map<Integer, ProtectionGuideTopic> slotTopics = new HashMap<>();

    public ProtectionGuideGUI(Player player, AbstractGUI parent) {
        super(player, parent);
    }

    @Override
    public void init() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "土地保護ガイド");
        }
        inventory.clear();
        fillDecoration();
        slotTopics.clear();

        putTopic(SLOT_ABOUT, ProtectionGuideTopic.ABOUT);
        putTopic(SLOT_AXE_USAGE, ProtectionGuideTopic.AXE_USAGE);
        putTopic(SLOT_RANGE, ProtectionGuideTopic.RANGE);
        putTopic(SLOT_CREATE, ProtectionGuideTopic.CREATE);
        putTopic(SLOT_CONFIRMATION, ProtectionGuideTopic.CONFIRMATION);

        putTopic(SLOT_FREE_LIMIT, ProtectionGuideTopic.FREE_LIMIT);
        putTopic(SLOT_PAYMENT, ProtectionGuideTopic.PAYMENT);
        putTopic(SLOT_RATES, ProtectionGuideTopic.RATES);
        putTopic(SLOT_REFUND, ProtectionGuideTopic.REFUND);

        putTopic(SLOT_MEMBERS, ProtectionGuideTopic.MEMBERS);
        putTopic(SLOT_SUBOWNERS, ProtectionGuideTopic.SUBOWNERS);
        putTopic(SLOT_PERMISSIONS, ProtectionGuideTopic.PERMISSIONS);
        putTopic(SLOT_FLAGS, ProtectionGuideTopic.FLAGS);
        putTopic(SLOT_CATEGORY, ProtectionGuideTopic.CATEGORY);
        putTopic(SLOT_TELEPORT, ProtectionGuideTopic.TELEPORT);
        putTopic(SLOT_LOGS, ProtectionGuideTopic.LOGS);
        putTopic(SLOT_DELETE, ProtectionGuideTopic.DELETE);

        putTopic(SLOT_FAQ, ProtectionGuideTopic.FAQ);
        inventory.setItem(SLOT_GET_AXE, axeItem());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "§l戻る")
                .lore(ChatColor.GRAY + "保護一覧へ戻ります")
                .build());
    }

    private void fillDecoration() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, slot < 9 || slot >= 45 ? border : background);
        }
    }

    private void putTopic(int slot, ProtectionGuideTopic topic) {
        slotTopics.put(slot, topic);
        inventory.setItem(slot, topicItem(topic));
    }

    private ItemStack topicItem(ProtectionGuideTopic topic) {
        String[] summary = switch (topic) {
            case ABOUT -> new String[]{"土地を自分のエリアとして", "安全に守る仕組みです"};
            case AXE_USAGE -> new String[]{"1点目を左クリック", "2点目を右クリックします"};
            case RANGE -> new String[]{"建物と出入口を含めて", "余裕をもって選びます"};
            case CREATE -> new String[]{"選択した範囲を確認して", "保護を作成します"};
            case CONFIRMATION -> new String[]{"作成前に範囲と料金を", "最後に確認できます"};
            case FREE_LIMIT -> new String[]{"一定量までは無料で", "土地を保護できます"};
            case PAYMENT -> new String[]{"無料枠を超えた分だけ", "料金が発生します"};
            case RATES -> new String[]{"広さに応じた価格帯を", "確認できます"};
            case REFUND -> new String[]{"削除時の返金条件を", "確認できます"};
            case MEMBERS -> new String[]{"一緒に使うメンバーを", "追加・管理できます"};
            case SUBOWNERS -> new String[]{"管理を任せる人を", "サブオーナーにできます"};
            case PERMISSIONS -> new String[]{"メンバーごとの権限を", "細かく設定できます"};
            case FLAGS -> new String[]{"PVPや建築などの設定を", "土地ごとに変更できます"};
            case CATEGORY -> new String[]{"保護を見やすい分類に", "整理できます"};
            case TELEPORT -> new String[]{"土地へ移動する地点を", "設定できます"};
            case LOGS -> new String[]{"土地内の主な操作履歴を", "確認できます"};
            case DELETE -> new String[]{"土地保護を削除します", "削除前に確認画面があります"};
            case FAQ -> new String[]{"困ったときの質問と回答を", "確認できます"};
        };
        return ItemStackBuilder.of(topic.material)
                .name(topic == ProtectionGuideTopic.DELETE ? ChatColor.RED + "§l保護を削除" : topic.title)
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + summary[0],
                        topic == ProtectionGuideTopic.DELETE ? ChatColor.RED + summary[1] : ChatColor.GRAY + summary[1],
                        ChatColor.YELLOW + "クリックで詳しく見る")
                .build();
    }

    private ItemStack axeItem() {
        return ItemStackBuilder.of(Material.GOLDEN_AXE)
                .name(ChatColor.GOLD + "§l金の斧を入手")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "ショッピングを開いて",
                        ChatColor.GRAY + "土地保護用の斧を入手します",
                        ChatColor.YELLOW + "クリックでショップを開く")
                .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() != this) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot == SLOT_BACK) {
            back();
            return;
        }
        if (slot == SLOT_GET_AXE) {
            openShop();
            return;
        }
        ProtectionGuideTopic topic = slotTopics.get(slot);
        if (topic != null) navigateTo(new ProtectionGuideDetailGUI(getPlayer(), this, topic.ordinal()));
    }

    private void openShop() {
        PlayerGuard.getInstance().getScheduler().runOnEntity(getPlayer(), () -> {
            getPlayer().closeInventory();
            if (!getPlayer().performCommand("openshop")) {
                getPlayer().sendMessage(ChatColor.RED + "ショップを開けませんでした。");
                getPlayer().sendMessage(ChatColor.GRAY + "管理者にお問い合わせください。");
            }
        });
    }
}