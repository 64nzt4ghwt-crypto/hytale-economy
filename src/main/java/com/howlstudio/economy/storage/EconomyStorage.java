package com.howlstudio.economy.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.howlstudio.economy.model.PlayerAccount;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

/**
 * Persists player balances as JSON.
 * File: plugins/Economy/balances.json
 */
public class EconomyStorage {

    private final Path dataFile;
    private final Gson gson;

    public EconomyStorage(Path dataDir) {
        this.dataFile = dataDir.resolve("balances.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        try { Files.createDirectories(dataDir); } catch (IOException ignored) {}
    }

    public synchronized List<StoredAccount> load() {
        if (!Files.exists(dataFile)) return new ArrayList<>();
        try (Reader r = Files.newBufferedReader(dataFile)) {
            Type t = new TypeToken<List<StoredAccount>>(){}.getType();
            List<StoredAccount> list = gson.fromJson(r, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[Economy] Failed to load balances: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void save(Collection<PlayerAccount> accounts) {
        List<StoredAccount> list = new ArrayList<>();
        for (PlayerAccount a : accounts) list.add(StoredAccount.from(a));
        try (Writer w = Files.newBufferedWriter(dataFile)) {
            gson.toJson(list, w);
        } catch (IOException e) {
            System.err.println("[Economy] Failed to save balances: " + e.getMessage());
        }
    }

    public static class StoredAccount {
        public String uuid;
        public String username;
        public long balance;
        public long totalEarned;
        public long totalSpent;
        public long createdAt;
        public long lastSeen;

        public static StoredAccount from(PlayerAccount a) {
            StoredAccount s = new StoredAccount();
            s.uuid        = a.getUuid().toString();
            s.username    = a.getUsername();
            s.balance     = a.getBalance();
            s.totalEarned = a.getTotalEarned();
            s.totalSpent  = a.getTotalSpent();
            s.createdAt   = a.getCreatedAt();
            s.lastSeen    = a.getLastSeen();
            return s;
        }

        public PlayerAccount toAccount() {
            PlayerAccount a = new PlayerAccount(UUID.fromString(uuid), username, balance);
            a.setCreatedAt(createdAt);
            a.setLastSeen(lastSeen);
            a.setBalance(balance);
            a.setTotalEarned(totalEarned);
            a.setTotalSpent(totalSpent);
            return a;
        }
    }
}
