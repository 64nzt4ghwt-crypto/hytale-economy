package com.howlstudio.economy;

import com.howlstudio.economy.config.EconomyConfig;
import com.howlstudio.economy.model.PlayerAccount;
import com.howlstudio.economy.storage.EconomyStorage;
import com.howlstudio.economy.storage.EconomyStorage.StoredAccount;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core economy manager — in-memory account store with JSON persistence.
 *
 * This is the public API other plugins can use via EconomyManager.getInstance().
 * Designed to be the economy foundation that other mods (auction house, shops) depend on.
 */
public class EconomyManager {

    private static EconomyManager instance;

    private final EconomyConfig config;
    private final EconomyStorage storage;

    /** All accounts, keyed by UUID string. */
    private final Map<String, PlayerAccount> accounts = new ConcurrentHashMap<>();

    /** Online players: username.lowercase → UUID string. */
    private final Map<String, String> nameToUuid = new ConcurrentHashMap<>();

    private EconomyManager(Path dataDir) {
        this.config  = new EconomyConfig(dataDir);
        this.storage = new EconomyStorage(dataDir);
        loadFromDisk();
    }

    public static void init(Path dataDir) {
        instance = new EconomyManager(dataDir);
    }

    public static EconomyManager getInstance() {
        return instance;
    }

    // ── Disk I/O ──────────────────────────────────────────────────────────────

    private void loadFromDisk() {
        List<StoredAccount> raw = storage.load();
        for (StoredAccount s : raw) {
            try {
                PlayerAccount a = s.toAccount();
                accounts.put(a.getUuid().toString(), a);
            } catch (Exception e) {
                System.err.println("[Economy] Skipped corrupt account: " + e.getMessage());
            }
        }
        System.out.println("[Economy] Loaded " + accounts.size() + " accounts.");
    }

    public void saveToDisk() {
        storage.save(accounts.values());
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────

    /**
     * Called when a player joins. Creates account if new, updates username.
     * @return the player's account
     */
    public PlayerAccount onPlayerJoin(UUID uuid, String username) {
        String key = uuid.toString();
        PlayerAccount acc = accounts.computeIfAbsent(key,
            k -> new PlayerAccount(uuid, username, config.getStartingBalance()));

        acc.setUsername(username);
        acc.setLastSeen(System.currentTimeMillis());
        nameToUuid.put(username.toLowerCase(), key);

        saveToDisk();
        return acc;
    }

    public void onPlayerLeave(UUID uuid, String username) {
        PlayerAccount acc = accounts.get(uuid.toString());
        if (acc != null) acc.setLastSeen(System.currentTimeMillis());
        if (username != null) nameToUuid.remove(username.toLowerCase());
        saveToDisk();
    }

    // ── Core Economy API ──────────────────────────────────────────────────────

    /** Get or create account for a UUID. */
    public PlayerAccount getAccount(UUID uuid) {
        return accounts.computeIfAbsent(uuid.toString(),
            k -> new PlayerAccount(uuid, "Unknown", config.getStartingBalance()));
    }

    /** Get balance for a UUID. */
    public long getBalance(UUID uuid) {
        return getAccount(uuid).getBalance();
    }

    /**
     * Transfer money from one player to another.
     * @return null on success, error message on failure.
     */
    public String transfer(UUID from, UUID to, long amount) {
        if (amount < config.getMinTransfer()) {
            return "Minimum transfer is " + config.format(config.getMinTransfer()) + ".";
        }
        if (amount <= 0) return "Amount must be positive.";

        PlayerAccount sender    = getAccount(from);
        PlayerAccount recipient = getAccount(to);

        if (!sender.has(amount)) {
            return "Insufficient funds. You have " + config.format(sender.getBalance()) + ".";
        }

        long newRecipient = recipient.getBalance() + amount;
        if (newRecipient > config.getMaxBalance()) {
            return "That would exceed the recipient's maximum balance ("
                + config.format(config.getMaxBalance()) + ").";
        }

        sender.withdraw(amount);
        recipient.deposit(amount);

        if (config.isLogTransactions()) {
            System.out.printf("[Economy] Transfer: %s → %s: %s%n",
                sender.getUsername(), recipient.getUsername(), config.format(amount));
        }

        saveToDisk();
        return null;
    }

    /**
     * Admin: add money to a player's account.
     * @return null on success, error on failure.
     */
    public String adminGive(UUID target, long amount) {
        if (amount <= 0) return "Amount must be positive.";
        PlayerAccount acc = getAccount(target);
        if (acc.getBalance() + amount > config.getMaxBalance()) {
            return "Would exceed max balance.";
        }
        acc.deposit(amount);
        saveToDisk();
        return null;
    }

    /**
     * Admin: remove money from a player's account.
     */
    public String adminTake(UUID target, long amount) {
        if (amount <= 0) return "Amount must be positive.";
        PlayerAccount acc = getAccount(target);
        if (!acc.has(amount) && !config.isAllowNegative()) {
            return "Player only has " + config.format(acc.getBalance()) + ".";
        }
        acc.withdraw(amount);
        saveToDisk();
        return null;
    }

    /**
     * Admin: set a player's balance directly.
     */
    public String adminSet(UUID target, long amount) {
        if (amount < 0 && !config.isAllowNegative()) return "Balance cannot be negative.";
        if (amount > config.getMaxBalance()) return "Exceeds max balance.";
        getAccount(target).setBalance(amount);
        saveToDisk();
        return null;
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /** Resolve online player name to UUID string. */
    public Optional<String> resolveUsername(String name) {
        return Optional.ofNullable(nameToUuid.get(name.toLowerCase()));
    }

    /** Get all accounts sorted by balance descending (for /baltop). */
    public List<PlayerAccount> getTopAccounts(int limit) {
        return accounts.values().stream()
            .sorted(Comparator.comparingLong(PlayerAccount::getBalance).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public EconomyConfig getConfig() { return config; }
    public int totalAccounts()       { return accounts.size(); }
}
