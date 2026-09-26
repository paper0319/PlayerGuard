package net.nekozouneko.playerguard.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.gui.ProtectionGuiText;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.Instant;

/** Shared, synchronous-on-region-thread delete and refund implementation for GUI and commands. */
public final class ProtectionDeletionService {
    public enum Result { DELETED, NOT_FOUND, DENIED, REFUND_UNAVAILABLE, REFUND_FAILED }

    private ProtectionDeletionService() {}

    public static Result delete(Player actor, ProtectedRegion region, World world, String source) {
        if (actor == null || region == null || world == null) return Result.NOT_FOUND;
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null || manager.getRegion(region.getId()) == null) return Result.NOT_FOUND;
        if (!ProtectionAccess.canDeleteProtection(actor, region)) return Result.DENIED;

        PlayerGuard plugin = PlayerGuard.getInstance();
        ProtectionPaymentRecord payment = plugin.getProtectionPaymentRepository().find(region);
        if (plugin.getPaidExtensionConfig().allowRefundOnDelete() && payment != null && !payment.refunded()
                && payment.amountPaid().signum() > 0 && payment.payer() != null) {
            BigDecimal refund = ProtectionGuiText.refund(payment.amountPaid(), payment.createdAt(), Instant.now());
            if (!plugin.getEconomyTransactionService().available()) return Result.REFUND_UNAVAILABLE;
            if (!plugin.getEconomyTransactionService().deposit(Bukkit.getOfflinePlayer(payment.payer()), refund)) return Result.REFUND_FAILED;
            plugin.getProtectionPaymentRepository().save(region, payment.refund());
            plugin.getProtectionLogService().log(region, "返金", actor.getUniqueId(), payment.payer().toString(),
                    payment.amountPaid().toPlainString(), refund.toPlainString(), "source=" + source);
        }
        plugin.getProtectionLogService().log(region, "保護削除完了", actor.getUniqueId(), null, null, null,
                "source=" + source + ";admin=" + ProtectionAccess.isAdmin(actor));
        if (plugin.getVisitorLogService() != null) plugin.getVisitorLogService().clearByRegionId(region.getId());
        if (plugin.getProtectionTeleportRepository() != null) plugin.getProtectionTeleportRepository().delete(region);
        manager.removeRegion(region.getId());
        return Result.DELETED;
    }
}
