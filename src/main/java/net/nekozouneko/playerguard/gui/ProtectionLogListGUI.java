package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionLogEntry;
import net.nekozouneko.playerguard.visitlog.VisitorLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ProtectionLogListGUI extends AbstractGUI {
    private static final int PAGE_SIZE=45; private final ProtectedRegion region; private final World world; private final ProtectionLogCategoryGUI.Category category; private final List<ProtectionLogEntry> entries=new ArrayList<>(); private int page; private boolean newestFirst=true;
    public ProtectionLogListGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world, ProtectionLogCategoryGUI.Category category){super(player,parent);this.region=region;this.world=world;this.category=category;}
    @Override public void init(){ if(inventory==null) inventory=Bukkit.createInventory(this,54,ChatColor.YELLOW+"保護ログ: "+category.name()); inventory.clear(); entries.clear(); entries.addAll(PlayerGuard.getInstance().getProtectionLogService().getEntries(region,false)); addVisitorEntries(); entries.removeIf(e->!category.matches(e.type())); entries.sort(Comparator.comparingLong(ProtectionLogEntry::at)); if(newestFirst) Collections.reverse(entries); org.bukkit.inventory.ItemStack glass=ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build(); for(int i=45;i<54;i++)inventory.setItem(i,glass); int max=entries.isEmpty()?0:(entries.size()-1)/PAGE_SIZE; page=Math.max(0,Math.min(page,max)); for(int i=0;i<PAGE_SIZE;i++){int idx=page*PAGE_SIZE+i; if(idx>=entries.size())break; inventory.setItem(i,item(entries.get(idx)));} if(entries.isEmpty()) inventory.setItem(22,ItemStackBuilder.of(Material.PAPER).name(ChatColor.GRAY+"このカテゴリのログはまだありません").build()); inventory.setItem(45,ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE+"カテゴリ選択へ戻る").build()); if(page>0)inventory.setItem(47,ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE+"前のページ").build()); inventory.setItem(49,ItemStackBuilder.of(Material.HOPPER).name(ChatColor.YELLOW+(newestFirst?"新しい順":"古い順")).build()); if(page<max)inventory.setItem(51,ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE+"次のページ").build()); inventory.setItem(53,ItemStackBuilder.of(Material.BARRIER).name(ChatColor.RED+"閉じる").build()); }
    private void addVisitorEntries(){ if(PlayerGuard.getInstance().getVisitorLogService()==null)return; for(VisitorLogEntry v: PlayerGuard.getInstance().getVisitorLogService().getEntries(world.getName(), region.getId())){ String type; switch(v.getType()){ case ENTER:type="プレイヤー入場";break; case EXIT:type="プレイヤー退場";break; default:type="訪問者ログ";break;} entries.add(new ProtectionLogEntry(v.getAt(),type,v.getPlayer(),null,null,null,region.getId(),v.getDetail())); } }
    private org.bukkit.inventory.ItemStack item(ProtectionLogEntry e){ OfflinePlayer op=e.actor()==null?null:Bukkit.getOfflinePlayer(e.actor()); String actor=op==null?"不明":(op.getName()!=null?op.getName():e.actor().toString()); return ItemStackBuilder.of(icon(e.type())).name(ChatColor.YELLOW+e.type()).lore(ChatColor.DARK_GRAY+"────────────", ChatColor.GRAY+"実行者: "+ChatColor.WHITE+actor, ChatColor.GRAY+"実行者UUID: "+ChatColor.WHITE+(e.actor()==null?"不明":e.actor()), ChatColor.GRAY+"対象: "+ChatColor.WHITE+blank(e.target()), ChatColor.GRAY+"変更前: "+ChatColor.WHITE+blank(e.before()), ChatColor.GRAY+"変更後: "+ChatColor.WHITE+blank(e.after()), ChatColor.GRAY+"日時: "+ChatColor.WHITE+ProtectionGuiText.date(Instant.ofEpochMilli(e.at())), ChatColor.GRAY+"保護ID: "+ChatColor.WHITE+e.regionId(), ChatColor.GRAY+"補足: "+ChatColor.WHITE+blank(e.detail()), ChatColor.DARK_GRAY+"────────────").build(); }
    private Material icon(String type){ if(type==null)return Material.PAPER; if(type.contains("返金")||type.contains("支払い"))return Material.EMERALD; if(type.contains("削除"))return Material.RED_CONCRETE; if(type.contains("メンバー"))return Material.PLAYER_HEAD; if(type.contains("フラグ"))return Material.REDSTONE_TORCH; if(type.contains("保護名"))return Material.NAME_TAG; return Material.PAPER; }
    private String blank(String v){return v==null||v.isBlank()?"-":v;}
    @EventHandler public void onClick(InventoryClickEvent e){ if(e.getView().getTopInventory().getHolder()!=this)return; e.setCancelled(true); switch(e.getRawSlot()){case 45:back();break;case 47:page--;init();break;case 49:newestFirst=!newestFirst;page=0;init();break;case 51:page++;init();break;case 53:getPlayer().closeInventory();break;default:break;} }
}