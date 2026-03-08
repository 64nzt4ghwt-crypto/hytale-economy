package com.howlstudio.economy.command;

import com.howlstudio.economy.EconomyManager;
import com.howlstudio.economy.config.EconomyConfig;
import com.howlstudio.economy.model.PlayerAccount;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * /money command — economy hub.
 *
 *   /money                  — show your balance
 *   /money bal [player]     — show a player's balance
 *   /money pay <player> <amount>  — send money
 *   /money top [page]       — richest players leaderboard
 *   /money give <player> <amount> — admin: give money
 *   /money take <player> <amount> — admin: remove money
 *   /money set <player> <amount>  — admin: set balance
 */
public class EconomyCommand extends AbstractPlayerCommand {

    private final EconomyManager manager;

    public EconomyCommand(EconomyManager manager) {
        super("money", "Economy commands — balance, pay, leaderboard.");
        this.manager = manager;
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {

        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+");
        String[] args  = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        if (args.length == 0) {
            doBalance(playerRef, null);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "bal", "balance" -> doBalance(playerRef, args.length > 1 ? args[1] : null);
            case "pay"            -> doPay(playerRef, args);
            case "top", "baltop"  -> doTop(playerRef, args);
            case "give"           -> doAdminGive(playerRef, args);
            case "take"           -> doAdminTake(playerRef, args);
            case "set"            -> doAdminSet(playerRef, args);
            default               -> sendHelp(playerRef);
        }
    }

    // ── Subcommands ───────────────────────────────────────────────────────────

    private void doBalance(PlayerRef playerRef, String targetName) {
        EconomyConfig cfg = manager.getConfig();

        if (targetName == null) {
            PlayerAccount acc = manager.getAccount(playerRef.getUuid());
            send(playerRef, "§6[Economy] §fBalance: §e" + cfg.format(acc.getBalance()));
            return;
        }

        Optional<String> uuidStr = manager.resolveUsername(targetName);
        if (uuidStr.isEmpty()) {
            send(playerRef, "§c" + targetName + " is not online.");
            return;
        }

        PlayerAccount acc = manager.getAccount(UUID.fromString(uuidStr.get()));
        send(playerRef, "§6[Economy] §e" + targetName + "§f's balance: §e" +
            cfg.format(acc.getBalance()));
    }

    private void doPay(PlayerRef playerRef, String[] args) {
        if (args.length < 3) { send(playerRef, "§cUsage: /money pay <player> <amount>"); return; }

        EconomyConfig cfg = manager.getConfig();
        String targetName = args[1];
        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            send(playerRef, "§cInvalid amount: " + args[2]);
            return;
        }

        Optional<String> targetUuidStr = manager.resolveUsername(targetName);
        if (targetUuidStr.isEmpty()) {
            send(playerRef, "§c" + targetName + " is not online.");
            return;
        }

        UUID targetUuid = UUID.fromString(targetUuidStr.get());
        if (targetUuid.equals(playerRef.getUuid())) {
            send(playerRef, "§cYou can't pay yourself.");
            return;
        }

        String error = manager.transfer(playerRef.getUuid(), targetUuid, amount);
        if (error != null) {
            send(playerRef, "§c[Economy] " + error);
            return;
        }

        long newBalance = manager.getBalance(playerRef.getUuid());
        send(playerRef, "§a[Economy] §fSent §e" + cfg.format(amount) +
            " §fto §e" + targetName + "§f. New balance: §e" + cfg.format(newBalance));

        // Notify recipient if online
        try {
            for (PlayerRef p : Universe.get().getPlayers()) {
                if (p.getUuid().equals(targetUuid)) {
                    p.sendMessage(Message.raw(
                        "§a[Economy] §e" + playerRef.getUsername() +
                        " §fsent you §e" + cfg.format(amount) + "§f. Balance: §e" +
                        cfg.format(manager.getBalance(targetUuid))
                    ));
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void doTop(PlayerRef playerRef, String[] args) {
        EconomyConfig cfg = manager.getConfig();
        int page = 1;
        if (args.length > 1) {
            try { page = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException ignored) {}
        }

        int pageSize = cfg.getBalTopPageSize();
        List<PlayerAccount> top = manager.getTopAccounts(pageSize * page);
        int start = (page - 1) * pageSize;
        int end   = Math.min(start + pageSize, top.size());

        if (start >= top.size()) {
            send(playerRef, "§cNo entries on page " + page + ".");
            return;
        }

        send(playerRef, "§6══ §eLEADERBOARD §8(page " + page + ") §6══");
        for (int i = start; i < end; i++) {
            PlayerAccount acc = top.get(i);
            send(playerRef, "§7" + (i + 1) + ". §e" + acc.getUsername() +
                " §f— §a" + cfg.format(acc.getBalance()));
        }
    }

    // ── Admin commands ────────────────────────────────────────────────────────

    private void doAdminGive(PlayerRef playerRef, String[] args) {
        if (!isAdmin(playerRef)) return;
        if (args.length < 3) { send(playerRef, "§cUsage: /money give <player> <amount>"); return; }

        UUID target = resolveTarget(playerRef, args[1]);
        if (target == null) return;

        try {
            long amount = Long.parseLong(args[2]);
            String error = manager.adminGive(target, amount);
            if (error != null) { send(playerRef, "§c" + error); return; }
            send(playerRef, "§a[Economy] §fGave §e" + manager.getConfig().format(amount) +
                " §fto §e" + args[1] + "§f.");
        } catch (NumberFormatException e) {
            send(playerRef, "§cInvalid amount.");
        }
    }

    private void doAdminTake(PlayerRef playerRef, String[] args) {
        if (!isAdmin(playerRef)) return;
        if (args.length < 3) { send(playerRef, "§cUsage: /money take <player> <amount>"); return; }

        UUID target = resolveTarget(playerRef, args[1]);
        if (target == null) return;

        try {
            long amount = Long.parseLong(args[2]);
            String error = manager.adminTake(target, amount);
            if (error != null) { send(playerRef, "§c" + error); return; }
            send(playerRef, "§a[Economy] §fRemoved §e" + manager.getConfig().format(amount) +
                " §ffrom §e" + args[1] + "§f.");
        } catch (NumberFormatException e) {
            send(playerRef, "§cInvalid amount.");
        }
    }

    private void doAdminSet(PlayerRef playerRef, String[] args) {
        if (!isAdmin(playerRef)) return;
        if (args.length < 3) { send(playerRef, "§cUsage: /money set <player> <amount>"); return; }

        UUID target = resolveTarget(playerRef, args[1]);
        if (target == null) return;

        try {
            long amount = Long.parseLong(args[2]);
            String error = manager.adminSet(target, amount);
            if (error != null) { send(playerRef, "§c" + error); return; }
            send(playerRef, "§a[Economy] §fSet §e" + args[1] + "§f's balance to §e" +
                manager.getConfig().format(amount) + "§f.");
        } catch (NumberFormatException e) {
            send(playerRef, "§cInvalid amount.");
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void sendHelp(PlayerRef playerRef) {
        send(playerRef, "§8══ §6Economy Commands §8══");
        send(playerRef, "§e/money §7— Your balance");
        send(playerRef, "§e/money bal [player] §7— Check balance");
        send(playerRef, "§e/money pay <player> <amount> §7— Send money");
        send(playerRef, "§e/money top [page] §7— Leaderboard");
        if (isAdminSilent(playerRef)) {
            send(playerRef, "§e/money give/take/set <player> <amount> §7— Admin");
        }
    }

    private boolean isAdmin(PlayerRef ref) {
        // Admin check via Hytale permissions module (PermissionsModule singleton)
        try {
            var permsModule = com.hypixel.hytale.server.core.permissions.PermissionsModule.get();
            if (permsModule != null) {
                var provider = permsModule.getFirstPermissionProvider();
                if (provider != null) {
                    var groups = provider.getGroupsForUser(ref.getUuid());
                    if (groups != null && (groups.contains("admin") || groups.contains("op"))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        send(ref, "§cYou don't have permission for admin economy commands.");
        return false;
    }

    private boolean isAdminSilent(PlayerRef ref) {
        try {
            var permsModule = com.hypixel.hytale.server.core.permissions.PermissionsModule.get();
            if (permsModule != null) {
                var provider = permsModule.getFirstPermissionProvider();
                if (provider != null) {
                    var groups = provider.getGroupsForUser(ref.getUuid());
                    return groups != null && (groups.contains("admin") || groups.contains("op"));
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private UUID resolveTarget(PlayerRef sender, String name) {
        Optional<String> uuidStr = manager.resolveUsername(name);
        if (uuidStr.isEmpty()) {
            send(sender, "§c" + name + " is not online.");
            return null;
        }
        return UUID.fromString(uuidStr.get());
    }

    private void send(PlayerRef ref, String text) {
        ref.sendMessage(Message.raw(text));
    }
}
