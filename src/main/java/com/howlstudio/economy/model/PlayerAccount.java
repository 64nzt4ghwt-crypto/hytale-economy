package com.howlstudio.economy.model;

import java.util.UUID;

/**
 * Represents a player's economy account.
 *
 * Balances are stored as longs (integer coin units) to avoid floating-point precision bugs.
 * Display uses the configured currency symbol.
 */
public class PlayerAccount {

    private final UUID uuid;
    private String username;
    private long balance;
    private long totalEarned;
    private long totalSpent;
    private long createdAt;
    private long lastSeen;

    public PlayerAccount(UUID uuid, String username, long startingBalance) {
        this.uuid       = uuid;
        this.username   = username;
        this.balance    = startingBalance;
        this.createdAt  = System.currentTimeMillis();
        this.lastSeen   = createdAt;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public synchronized boolean deposit(long amount) {
        if (amount <= 0) return false;
        balance    += amount;
        totalEarned += amount;
        return true;
    }

    public synchronized boolean withdraw(long amount) {
        if (amount <= 0) return false;
        if (balance < amount) return false;
        balance    -= amount;
        totalSpent += amount;
        return true;
    }

    public synchronized boolean has(long amount) {
        return balance >= amount;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getUuid()          { return uuid; }
    public String getUsername()    { return username; }
    public void setUsername(String n) { this.username = n; }
    public long getBalance()       { return balance; }
    public long getTotalEarned()   { return totalEarned; }
    public long getTotalSpent()    { return totalSpent; }
    public long getCreatedAt()     { return createdAt; }
    public long getLastSeen()      { return lastSeen; }
    public void setLastSeen(long t){ this.lastSeen = t; }
    public void setCreatedAt(long t){ this.createdAt = t; }
    public void setBalance(long b) { this.balance = b; }
    public void setTotalEarned(long v) { this.totalEarned = v; }
    public void setTotalSpent(long v)  { this.totalSpent = v; }
}
