package net.nekozouneko.playerguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.Getter;
import net.nekozouneko.playerguard.command.*;
import net.nekozouneko.playerguard.command.sub.playerguard.ConfirmCommand;
import net.nekozouneko.playerguard.flag.GuardIgnoredFlag;
import net.nekozouneko.playerguard.flag.GuardRegisteredFlag;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.gui.AbstractGUI;
import net.nekozouneko.playerguard.gui.ChatInputManager;
import net.nekozouneko.playerguard.gui.ProtectionTeleportGUI;
import net.nekozouneko.playerguard.listener.DeniedEntryListener;
import net.nekozouneko.playerguard.listener.PlayerChangedWorldListener;
import net.nekozouneko.playerguard.listener.PlayerInteractListener;
import net.nekozouneko.playerguard.listener.ProtectionBlacklistListener;
import net.nekozouneko.playerguard.listener.VisitorLogListener;
import net.nekozouneko.playerguard.paid.CreationConfirmationManager;
import net.nekozouneko.playerguard.paid.CreationConfirmationSettings;
import net.nekozouneko.playerguard.paid.EconomyTransactionService;
import net.nekozouneko.playerguard.paid.PaidExtensionConfig;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRepository;
import net.nekozouneko.playerguard.paid.ProtectionNameRepository;
import net.nekozouneko.playerguard.paid.ProtectionLogService;
import net.nekozouneko.playerguard.paid.ProtectionTeleportRepository;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.paid.ProgressivePricingService;
import net.nekozouneko.playerguard.scheduler.PGScheduler;
import net.nekozouneko.playerguard.selection.SelectionStorage;
import net.nekozouneko.playerguard.task.ActionbarTask;
import net.nekozouneko.playerguard.task.RentalExpiryTask;
import net.nekozouneko.playerguard.task.SelectionRenderTask;
import net.nekozouneko.playerguard.task.VisitorLogFlushTask;
import net.nekozouneko.playerguard.visitlog.VisitorLogService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public final class PlayerGuard extends JavaPlugin {

    @Getter
    private static PlayerGuard instance;
    @Getter
    private static StateFlag guardRegisteredFlag;
    @Getter
    private static StateFlag guardIgnoredFlag;
    private static final int PROTECTION_LIMIT_BASE_VALUE = 30000;

    @Getter
    private SelectionStorage selectionStorage;
    @Getter
    private VisitorLogService visitorLogService;
    @Getter
    private PGScheduler scheduler;
    @Getter private PaidExtensionConfig paidExtensionConfig;
    @Getter private ProgressivePricingService progressivePricingService;
    @Getter private EconomyTransactionService economyTransactionService;
    @Getter private ProtectionPaymentRepository protectionPaymentRepository;
    @Getter private ProtectionNameRepository protectionNameRepository;
    @Getter private ProtectionCustomizationRepository protectionCustomizationRepository;
    @Getter private ProtectionLogService protectionLogService;
    @Getter private ProtectionTeleportRepository protectionTeleportRepository;
    @Getter private CreationConfirmationSettings creationConfirmationSettings;
    @Getter private CreationConfirmationManager creationConfirmationManager;
    private volatile boolean runtimeReady;

    @Override
    public void onLoad() {
        getLogger().info("Registering worldguard flag...");
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            guardRegisteredFlag = new GuardRegisteredFlag();
            registry.register(guardRegisteredFlag);
        }
        catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pguard-registered");
            if (alreadyRegistered instanceof GuardRegisteredFlag) {
                guardRegisteredFlag = (GuardRegisteredFlag) alreadyRegistered;
            }
            else throw fce;
        }

        try {
            guardIgnoredFlag = new GuardIgnoredFlag();
            registry.register(guardIgnoredFlag);
        }
        catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pguard-ignored");
            if (alreadyRegistered instanceof GuardIgnoredFlag) {
                guardIgnoredFlag = (GuardIgnoredFlag) alreadyRegistered;
            }
            else throw fce;
        }

        try {
            registry.register(PGCustomFlags.PRIMARY_OWNER);
        }
        catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pg-primary-owner");
            if (alreadyRegistered instanceof StringFlag) {
                PGCustomFlags.PRIMARY_OWNER = (StringFlag) alreadyRegistered;
            }
            else throw fce;
        }

        try {
            registry.register(PGCustomFlags.PAYMENT_RECORD);
        } catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pg-payment-record");
            if (alreadyRegistered instanceof StringFlag) PGCustomFlags.PAYMENT_RECORD = (StringFlag) alreadyRegistered;
            else throw fce;
        }

        try {
            registry.register(PGCustomFlags.DISPLAY_NAME);
        } catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pg-display-name");
            if (alreadyRegistered instanceof StringFlag) PGCustomFlags.DISPLAY_NAME = (StringFlag) alreadyRegistered;
            else throw fce;
        }

        registerStringFlag(registry, PGCustomFlags.CUSTOM_CATEGORY, "pg-category", flag -> PGCustomFlags.CUSTOM_CATEGORY = flag);
        registerStringFlag(registry, PGCustomFlags.CUSTOM_COLOR, "pg-color", flag -> PGCustomFlags.CUSTOM_COLOR = flag);
        registerStringFlag(registry, PGCustomFlags.PROTECTION_LOGS, "pg-logs", flag -> PGCustomFlags.PROTECTION_LOGS = flag);

        try {
            registry.register(PGCustomFlags.RENTALS);
        }
        catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pg-rentals");
            if (alreadyRegistered instanceof SetFlag) {
                @SuppressWarnings("unchecked")
                SetFlag<String> sf = (SetFlag<String>) alreadyRegistered;
                PGCustomFlags.RENTALS = sf;
            }
            else throw fce;
        }

        try {
            registry.register(PGCustomFlags.BLACKLIST);
        }
        catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get("pg-blacklist");
            if (alreadyRegistered instanceof SetFlag) {
                @SuppressWarnings("unchecked")
                SetFlag<String> sf = (SetFlag<String>) alreadyRegistered;
                PGCustomFlags.BLACKLIST = sf;
            }
            else throw fce;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        startRuntime(true);
    }

    @Override
    public void onDisable() {
        stopRuntime(true);
        instance = null;
    }

    private void reloadPaidExtensionServices() {
        paidExtensionConfig = PaidExtensionConfig.load(getConfig(), getLogger());
        progressivePricingService = new ProgressivePricingService(paidExtensionConfig.rates());
        protectionPaymentRepository = new ProtectionPaymentRepository();
        protectionNameRepository = new ProtectionNameRepository();
        protectionCustomizationRepository = new ProtectionCustomizationRepository();
        protectionLogService = new ProtectionLogService();
        protectionTeleportRepository = new ProtectionTeleportRepository(this);
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        economyTransactionService = new EconomyTransactionService(provider == null ? null : provider.getProvider());
        if (paidExtensionConfig.enabled() && !economyTransactionService.available())
            getLogger().warning("Paid extension is enabled but no Vault Economy provider is available; paid claims are disabled.");
    }

    public boolean isRuntimeReady() {
        return runtimeReady && isEnabled();
    }

    public void reload() {
        stopRuntime(false);
        startRuntime(false);
    }

    private void startRuntime(boolean registerCommands) {
        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        PGConfig.setConfig(getConfig());
        reloadPaidExtensionServices();
        scheduler = PGScheduler.create(this);
        if (creationConfirmationSettings == null) creationConfirmationSettings = new CreationConfirmationSettings(this);
        creationConfirmationSettings.load();
        creationConfirmationManager = new CreationConfirmationManager(this);

        if (PGConfig.isVisitorLogEnabled()) {
            visitorLogService = new VisitorLogService(this, PGConfig.getVisitorLogMaxEntriesPerRegion());
        } else {
            visitorLogService = null;
        }
        if (selectionStorage == null) selectionStorage = new SelectionStorage();

        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new DeniedEntryListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectionBlacklistListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInputManager(), this);
        if (visitorLogService != null) {
            getServer().getPluginManager().registerEvents(new VisitorLogListener(visitorLogService), this);
        }
        getServer().getPluginManager().registerEvents(creationConfirmationManager, this);
        scheduler.runTimer(new ActionbarTask(), 0, 20);
        scheduler.runTimer(new SelectionRenderTask(), 0, 10);
        scheduler.runTimer(new RentalExpiryTask(), 20L * 60, 20L * 60);
        if (visitorLogService != null) {
            scheduler.runAsyncTimer(new VisitorLogFlushTask(visitorLogService),
                    PGConfig.getVisitorLogFlushIntervalTicks(), PGConfig.getVisitorLogFlushIntervalTicks());
        }
        if (registerCommands) registerCommands();
        runtimeReady = true;
    }

    private void stopRuntime(boolean fullShutdown) {
        runtimeReady = false;
        closePluginGuis();
        if (creationConfirmationManager != null) {
            if (fullShutdown) creationConfirmationManager.clearAll();
            else creationConfirmationManager.cancelAllForReload();
        }
        ChatInputManager.clear();
        ProtectionTeleportGUI.clearLocks();
        ConfirmCommand.clearConfirms();
        if (creationConfirmationSettings != null) creationConfirmationSettings.save();
        if (visitorLogService != null) visitorLogService.save();
        HandlerList.unregisterAll(this);
        if (scheduler != null) {
            scheduler.cancelAll();
            scheduler = null;
        }
        if (fullShutdown) {
            visitorLogService = null;
            selectionStorage = null;
            creationConfirmationManager = null;
            creationConfirmationSettings = null;
        }
    }

    private void registerCommands() {
        getCommand("cancel-claim").setExecutor(new CancelCommand());
        getCommand("claim").setExecutor(new ClaimCommand());
        getCommand("disclaim").setExecutor(new DisclaimCommand());
        getCommand("flags").setExecutor(new FlagsCommand());
        PlayerGuardCommand playerGuardCommand = new PlayerGuardCommand();
        getCommand("playerguard").setExecutor(playerGuardCommand);
        getCommand("playerguard").setTabCompleter(playerGuardCommand);
        PlayerGuardAdminCommand playerGuardAdminCommand = new PlayerGuardAdminCommand();
        getCommand("playerguard-admin").setExecutor(playerGuardAdminCommand);
        getCommand("playerguard-admin").setTabCompleter(playerGuardAdminCommand);
    }

    private void closePluginGuis() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Runnable close = () -> {
                InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof AbstractGUI) player.closeInventory();
            };
            if (scheduler != null && !scheduler.isOwnedByCurrentRegion(player)) scheduler.runOnEntity(player, close);
            else close.run();
        }
    }

    public long getProtectLimit(Player player) {
        int days = (player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 / 60 / 24;
        long limit = PGConfig.getLimit(days);

        NamespacedKey key = new NamespacedKey(this, "limit-extends");
        Long extend = player.getPersistentDataContainer().get(key, PersistentDataType.LONG);

        if (extend != null) {
            return limit + extend;
        }

        return limit;
    }

    public long getProtectionUsed(Player player) {
        return PGUtil.primaryOwnedVolume(PGUtil.getPlayerRegions(player).keySet(), player.getUniqueId());
    }

    public void resetAllRegions() {
        scheduler.runGlobal(() -> {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            rc.getLoaded().forEach(rm ->
                    rm.getRegions().values().stream()
                            .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                            .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                            .forEach(pr -> rm.removeRegion(pr.getId()))
            );
        });
    }

    private interface StringFlagSetter { void set(StringFlag flag); }

    private void registerStringFlag(FlagRegistry registry, StringFlag flag, String id, StringFlagSetter setter) {
        try {
            registry.register(flag);
        } catch (FlagConflictException fce) {
            Flag<?> alreadyRegistered = registry.get(id);
            if (alreadyRegistered instanceof StringFlag) setter.set((StringFlag) alreadyRegistered);
            else throw fce;
        }
    }

}
