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
import net.nekozouneko.playerguard.listener.PlayerChangedWorldListener;
import net.nekozouneko.playerguard.listener.PlayerInteractListener;
import net.nekozouneko.playerguard.listener.VisitorLogListener;
import net.nekozouneko.playerguard.paid.EconomyTransactionService;
import net.nekozouneko.playerguard.paid.PaidExtensionConfig;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRepository;
import net.nekozouneko.playerguard.paid.ProgressivePricingService;
import net.nekozouneko.playerguard.scheduler.PGScheduler;
import net.nekozouneko.playerguard.selection.SelectionStorage;
import net.nekozouneko.playerguard.task.ActionbarTask;
import net.nekozouneko.playerguard.task.RentalExpiryTask;
import net.nekozouneko.playerguard.task.SelectionRenderTask;
import net.nekozouneko.playerguard.task.VisitorLogFlushTask;
import net.nekozouneko.playerguard.visitlog.VisitorLogService;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
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
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        PGConfig.setConfig(getConfig());
        reloadPaidExtensionServices();

        if (PGConfig.isVisitorLogEnabled()) {
            visitorLogService = new VisitorLogService(this, PGConfig.getVisitorLogMaxEntriesPerRegion());
        } else {
            visitorLogService = null;
        }
        selectionStorage = new SelectionStorage();

        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        if (visitorLogService != null)
            getServer().getPluginManager().registerEvents(new VisitorLogListener(visitorLogService), this);

        scheduler = PGScheduler.create(this);
        scheduler.runTimer(new ActionbarTask(), 0, 20);
        scheduler.runTimer(new SelectionRenderTask(), 0, 10);
        scheduler.runTimer(new RentalExpiryTask(), 20L * 60, 20L * 60);
        if (visitorLogService != null) {
            scheduler.runAsyncTimer(new VisitorLogFlushTask(visitorLogService),
                    PGConfig.getVisitorLogFlushIntervalTicks(), PGConfig.getVisitorLogFlushIntervalTicks());
        }

        getCommand("cancel-claim").setExecutor(new CancelCommand());
        getCommand("claim").setExecutor(new ClaimCommand());
        getCommand("disclaim").setExecutor(new DisclaimCommand());
        getCommand("flags").setExecutor(new FlagsCommand());
        getCommand("playerguard").setExecutor(new PlayerGuardCommand());
        getCommand("playerguard-admin").setExecutor(new PlayerGuardAdminCommand());
    }

    @Override
    public void onDisable() {
        instance = null;

        if (scheduler != null) scheduler.cancelAll();
        scheduler = null;
        if (visitorLogService != null) visitorLogService.save();
        visitorLogService = null;

        ConfirmCommand.clearConfirms();
    }

    private void reloadPaidExtensionServices() {
        paidExtensionConfig = PaidExtensionConfig.load(getConfig(), getLogger());
        progressivePricingService = new ProgressivePricingService(paidExtensionConfig.rates());
        protectionPaymentRepository = new ProtectionPaymentRepository();
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        economyTransactionService = new EconomyTransactionService(provider == null ? null : provider.getProvider());
        if (paidExtensionConfig.enabled() && !economyTransactionService.available())
            getLogger().warning("Paid extension is enabled but no Vault Economy provider is available; paid claims are disabled.");
    }

    public void reload() {
        reloadConfig();
        PGConfig.setConfig(getConfig());
        reloadPaidExtensionServices();
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
        long used = 0;

        for (ProtectedRegion region : PGUtil.getPlayerRegions(player).keySet())
            used += region.volume();

        return used;
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
}
