package com.howlstudio.economy.config;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;

/**
 * Economy plugin configuration.
 * Auto-generated at plugins/Economy/economy-config.json on first run.
 */
public class EconomyConfig {

    private boolean enabled           = true;
    private String currencyName       = "Coins";
    private String currencySymbol     = "✦";
    private long startingBalance      = 500L;
    private long maxBalance           = 1_000_000_000L;
    private long minTransfer          = 1L;
    private boolean logTransactions   = true;
    private boolean allowNegative     = false;
    private int balTopPageSize        = 10;

    public EconomyConfig(Path dataDir) {
        Path file = dataDir.resolve("economy-config.json");

        if (!Files.exists(file)) {
            writeDefaults(file);
            return;
        }

        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("enabled"))           enabled           = o.get("enabled").getAsBoolean();
            if (o.has("currencyName"))      currencyName      = o.get("currencyName").getAsString();
            if (o.has("currencySymbol"))    currencySymbol    = o.get("currencySymbol").getAsString();
            if (o.has("startingBalance"))   startingBalance   = o.get("startingBalance").getAsLong();
            if (o.has("maxBalance"))        maxBalance        = o.get("maxBalance").getAsLong();
            if (o.has("minTransfer"))       minTransfer       = o.get("minTransfer").getAsLong();
            if (o.has("logTransactions"))   logTransactions   = o.get("logTransactions").getAsBoolean();
            if (o.has("allowNegative"))     allowNegative     = o.get("allowNegative").getAsBoolean();
            if (o.has("balTopPageSize"))    balTopPageSize    = o.get("balTopPageSize").getAsInt();
        } catch (Exception e) {
            System.err.println("[Economy] Config parse error — using defaults: " + e.getMessage());
        }
    }

    private void writeDefaults(Path file) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled",           enabled);
        o.addProperty("currencyName",      currencyName);
        o.addProperty("currencySymbol",    currencySymbol);
        o.addProperty("startingBalance",   startingBalance);
        o.addProperty("maxBalance",        maxBalance);
        o.addProperty("minTransfer",       minTransfer);
        o.addProperty("logTransactions",   logTransactions);
        o.addProperty("allowNegative",     allowNegative);
        o.addProperty("balTopPageSize",    balTopPageSize);
        try (Writer w = Files.newBufferedWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(o, w);
        } catch (IOException e) {
            System.err.println("[Economy] Failed to write default config: " + e.getMessage());
        }
    }

    /** Format a balance for display: "✦ 1,500" */
    public String format(long amount) {
        return currencySymbol + " " + String.format("%,d", amount);
    }

    public boolean isEnabled()          { return enabled; }
    public String getCurrencyName()     { return currencyName; }
    public String getCurrencySymbol()   { return currencySymbol; }
    public long getStartingBalance()    { return startingBalance; }
    public long getMaxBalance()         { return maxBalance; }
    public long getMinTransfer()        { return minTransfer; }
    public boolean isLogTransactions()  { return logTransactions; }
    public boolean isAllowNegative()    { return allowNegative; }
    public int getBalTopPageSize()      { return balTopPageSize; }
}
