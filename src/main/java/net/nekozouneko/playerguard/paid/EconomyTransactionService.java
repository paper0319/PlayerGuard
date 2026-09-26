package net.nekozouneko.playerguard.paid;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EconomyTransactionService {
    private final Economy economy;
    public EconomyTransactionService(Economy economy) { this.economy = economy; }
    public boolean available() { return economy != null; }
    public double balance(Player player) { return economy.getBalance(player); }
    public boolean withdraw(Player player, BigDecimal amount) { return response(economy.withdrawPlayer(player, amount(amount))); }
    public boolean deposit(Player player, BigDecimal amount) { return response(economy.depositPlayer(player, amount(amount))); }
    public boolean deposit(OfflinePlayer player, BigDecimal amount) { return response(economy.depositPlayer(player, amount(amount))); }
    private static boolean response(EconomyResponse response) { return response != null && response.transactionSuccess(); }
    private static double amount(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).doubleValue(); }
}
