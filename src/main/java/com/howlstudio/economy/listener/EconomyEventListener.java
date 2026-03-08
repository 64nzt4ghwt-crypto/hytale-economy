package com.howlstudio.economy.listener;

import com.howlstudio.economy.EconomyManager;
import com.howlstudio.economy.config.EconomyConfig;
import com.howlstudio.economy.model.PlayerAccount;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Handles player join/leave to create/update economy accounts.
 */
public class EconomyEventListener {

    private final EconomyManager manager;

    public EconomyEventListener(EconomyManager manager) {
        this.manager = manager;
    }

    public void register() {
        var bus = HytaleServer.get().getEventBus();
        bus.registerGlobal(PlayerReadyEvent.class,     this::onPlayerReady);
        bus.registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    @SuppressWarnings("deprecation")
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PlayerRef ref = player.getPlayerRef();
        if (ref == null) return;

        String name = ref.getUsername() != null ? ref.getUsername() : ref.getUuid().toString();
        boolean isNew = !manager.getAccount(ref.getUuid()).getUsername().equals(name);

        PlayerAccount acc = manager.onPlayerJoin(ref.getUuid(), name);
        com.howlstudio.economy.config.EconomyConfig cfg = manager.getConfig();

        if (isNew) {
            ref.sendMessage(Message.raw(
                "§6[Economy] §fWelcome! You received §e" +
                cfg.format(cfg.getStartingBalance()) +
                " §fto start with."
            ));
        }

        ref.sendMessage(Message.raw(
            "§6[Economy] §fBalance: §e" + cfg.format(acc.getBalance())
        ));
    }

    @SuppressWarnings("deprecation")
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef ref = event.getPlayerRef();
        if (ref == null) return;
        manager.onPlayerLeave(ref.getUuid(), ref.getUsername());
    }


}
