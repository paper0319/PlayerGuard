package net.nekozouneko.playerguard.gui;

import lombok.Getter;
import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class AbstractGUI implements Listener, InventoryHolder {

    @Getter
    private final Player player;
    @Getter
    private final AbstractGUI parent;
    protected Inventory inventory;
    private boolean registered = false;

    public AbstractGUI(Player player) {
        this(player, null);
    }

    public AbstractGUI(Player player, AbstractGUI parent) {
        this.player = player;
        this.parent = parent;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void init();

    public void open() {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.isRuntimeReady()) return;
        init();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    protected final void navigateTo(AbstractGUI next) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.isRuntimeReady() || plugin.getScheduler() == null) return;
        plugin.getScheduler().runOnEntity(player, next::open);
    }

    public void back() {
        if (parent != null) {
            navigateTo(parent);
        } else {
            player.closeInventory();
        }
    }

    protected void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler
    public void onAbstractClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        closeIfRuntimeStopped(e.getWhoClicked());
    }

    @EventHandler
    public void onAbstractDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        closeIfRuntimeStopped(e.getWhoClicked());
    }

    private void closeIfRuntimeStopped(org.bukkit.entity.HumanEntity who) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.isRuntimeReady()) who.closeInventory();
    }

    @EventHandler
    public void onAbstractClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) return;
        unregister();
    }

    @EventHandler
    public void onAbstractQuit(PlayerQuitEvent e) {
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }
}