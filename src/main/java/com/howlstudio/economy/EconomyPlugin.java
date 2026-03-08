package com.howlstudio.economy;

import com.howlstudio.economy.command.EconomyCommand;
import com.howlstudio.economy.listener.EconomyEventListener;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * EconomyPlugin — Virtual currency system for Hytale servers.
 *
 * Provides the economy foundation other plugins depend on.
 * Drop-in, zero-config, works immediately.
 *
 * Commands: /money [subcommand] [args]
 * Config:   plugins/Economy/economy-config.json
 * Data:     plugins/Economy/balances.json
 *
 * @version 1.0.0
 */
public final class EconomyPlugin extends JavaPlugin {

    public EconomyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        log("[Economy] Loading...");

        EconomyManager.init(getDataDirectory());
        EconomyManager manager = EconomyManager.getInstance();

        if (!manager.getConfig().isEnabled()) {
            log("[Economy] Disabled in config.");
            return;
        }

        new EconomyEventListener(manager).register();
        CommandManager.get().register(new EconomyCommand(manager));

        log("[Economy] Ready! " + manager.totalAccounts() + " accounts loaded.");
        log("[Economy] Currency: " + manager.getConfig().getCurrencyName() +
            " (" + manager.getConfig().getCurrencySymbol() + ")");
        log("[Economy] Starting balance: " +
            manager.getConfig().format(manager.getConfig().getStartingBalance()));
    }

    @Override
    protected void shutdown() {
        EconomyManager mgr = EconomyManager.getInstance();
        if (mgr != null) {
            mgr.saveToDisk();
            log("[Economy] Balances saved.");
        }
        log("[Economy] Shutdown complete.");
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
