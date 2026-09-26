package net.nekozouneko.playerguard.gui;

import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.CreationConfirmationManager;
import net.nekozouneko.playerguard.paid.ProtectionPricingPreview;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** GUI is only a view; closing it leaves the manager-owned pending creation intact. */
public class ClaimConfirmGUI extends AbstractGUI {
    private static final int SLOT_INFO=13, SLOT_CREATE=11, SLOT_CANCEL=15;
    public ClaimConfirmGUI(Player player) { super(player, null); }
    @Override public void init() {
        if(inventory==null) inventory=Bukkit.createInventory(this,27,ChatColor.BLACK+"土地保護の最終確認"); inventory.clear();
        org.bukkit.inventory.ItemStack glass=ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build(); for(int i=0;i<27;i++) inventory.setItem(i,glass);
        inventory.setItem(SLOT_INFO,ItemStackBuilder.of(Material.PAPER).name(ChatColor.AQUA+"土地保護の確認内容").lore(lore()).build());
        inventory.setItem(SLOT_CREATE,ItemStackBuilder.of(Material.LIME_CONCRETE).name(ChatColor.GREEN+"この内容で保護を作成").lore(ChatColor.GRAY+"/claim・/hogo と同じ処理です").build());
        inventory.setItem(SLOT_CANCEL,ItemStackBuilder.of(Material.RED_CONCRETE).name(ChatColor.RED+"キャンセル").lore(ChatColor.GRAY+"/cancel と同じ処理です").build());
    }
    private String[] lore(){
        CreationConfirmationManager.PendingProtectionCreation s=PlayerGuard.getInstance().getCreationConfirmationManager().getPending(getPlayer().getUniqueId()); if(s==null)return new String[]{ChatColor.RED+"確認待ちの土地保護がありません。"}; ProtectionPricingPreview.Result p=s.preview; BigDecimal balance=PlayerGuard.getInstance().getEconomyTransactionService().available()?BigDecimal.valueOf(PlayerGuard.getInstance().getEconomyTransactionService().balance(getPlayer())):BigDecimal.ZERO; List<String> l=new ArrayList<>();
        l.add(ChatColor.GRAY+"ワールド: "+ChatColor.WHITE+s.worldName);l.add(ChatColor.GRAY+"開始地点: "+ChatColor.WHITE+pos(s.selection.getPos1()));l.add(ChatColor.GRAY+"終了地点: "+ChatColor.WHITE+pos(s.selection.getPos2()));l.add(ChatColor.GRAY+"範囲: "+ChatColor.WHITE+s.width+" × "+s.height+" × "+s.depth);l.add(ChatColor.GRAY+"体積: "+ChatColor.YELLOW+ProtectionGuiText.blocks(s.volume));l.add(ChatColor.GRAY+"無料対象: "+ChatColor.GREEN+ProtectionGuiText.blocks(s.freeVolume));l.add(ChatColor.GRAY+"有料対象: "+ChatColor.RED+ProtectionGuiText.blocks(s.paidVolume));l.add(ChatColor.GRAY+"価格内訳:");for(ProtectionPricingPreview.Band b:p.bands())l.add(ChatColor.WHITE+String.format("%,d～%,d: %,d × %s = %s",b.from(),b.to(),b.volume(),ProtectionGuiText.rate(b.rate()),ProtectionGuiText.money(b.amount())));
        if(getPlayer().hasPermission("playerguard.admin.free-protection")){l.add(ChatColor.GREEN+"管理者無料保護");l.add(ChatColor.GRAY+"本来必要だった金額: "+ChatColor.YELLOW+ProtectionGuiText.money(p.total()));l.add(ChatColor.GRAY+"実際の支払金額: "+ChatColor.GREEN+ProtectionGuiText.money(BigDecimal.ZERO));}else l.add(ChatColor.GRAY+"必要金額: "+ChatColor.YELLOW+ProtectionGuiText.money(p.total()));l.add(ChatColor.GRAY+"所持金: "+ChatColor.WHITE+ProtectionGuiText.money(balance));l.add(ChatColor.GRAY+"作成後残高: "+ChatColor.WHITE+ProtectionGuiText.money(balance.subtract(getPlayer().hasPermission("playerguard.admin.free-protection")?BigDecimal.ZERO:p.total())));return l.toArray(new String[0]); }
    private String pos(com.sk89q.worldedit.math.BlockVector3 p){return "X"+p.x()+" Y"+p.y()+" Z"+p.z();}
    @EventHandler public void onClick(InventoryClickEvent e){if(e.getView().getTopInventory().getHolder()!=this)return;e.setCancelled(true);if(e.getRawSlot()==SLOT_CANCEL){getPlayer().closeInventory();PlayerGuard.getInstance().getCreationConfirmationManager().cancel(getPlayer());return;}if(e.getRawSlot()==SLOT_CREATE){getPlayer().closeInventory();PlayerGuard.getInstance().getCreationConfirmationManager().confirm(getPlayer());}}
}