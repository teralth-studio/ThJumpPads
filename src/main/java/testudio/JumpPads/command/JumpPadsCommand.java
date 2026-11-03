package testudio.JumpPads.command;

import testudio.JumpPads.JumpPadsPlugin;
import testudio.JumpPads.storage.StorageException;
import testudio.JumpPads.util.ConsoleMessages;
import testudio.JumpPads.util.PlaceholderSupport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class JumpPadsCommand implements CommandExecutor, TabCompleter {

    private final JumpPadsPlugin plugin;

    public JumpPadsCommand(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            plugin.messages().send(sender, "unknown-command", Collections.singletonMap("command", label));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!sender.hasPermission("thjumppads.reload")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            if (!plugin.toggles().isReady()) {
                plugin.messages().send(sender, "storage-loading");
                return true;
            }
            PlaceholderSupport.reset();
            long reloadStartedAt = System.nanoTime();
            try {
                plugin.reloadAll();
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Storage reload failed: &b" + e.detailedMessage());
                plugin.messages().send(sender, "storage-error");
                return true;
            }
            String reloadTime = Long.toString((System.nanoTime() - reloadStartedAt) / 1_000_000L);
            plugin.messages().send(sender, "reloaded",
                    Collections.singletonMap("time", reloadTime));
            return true;
        }

        if (!plugin.toggles().isReady()) {
            plugin.messages().send(sender, "storage-loading");
            return true;
        }

        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;

        switch (sub) {
            case "toggle":
                boolean now = plugin.toggles().toggle(player.getUniqueId(), player.getName());
                plugin.messages().send(player, now ? "enabled" : "disabled");
                break;
            case "enable":
            case "on":
                if (plugin.toggles().isEnabled(player.getUniqueId(), player.getName())) {
                    plugin.messages().send(player, "already-enabled");
                } else {
                    plugin.toggles().setEnabled(player.getUniqueId(), player.getName(), true);
                    plugin.messages().send(player, "enabled");
                }
                break;
            case "disable":
            case "off":
                if (!plugin.toggles().isEnabled(player.getUniqueId(), player.getName())) {
                    plugin.messages().send(player, "already-disabled");
                } else {
                    plugin.toggles().setEnabled(player.getUniqueId(), player.getName(), false);
                    plugin.messages().send(player, "disabled");
                }
                break;
            case "status":
                plugin.messages().send(player, "status");
                break;
            default:
                plugin.messages().send(player, "unknown-command",
                        Collections.singletonMap("command", label));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (String option : Arrays.asList("toggle", "enable", "disable", "status", "reload")) {
                if (option.equals("reload") && !sender.hasPermission("thjumppads.reload")) {
                    continue;
                }
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
            }
        }
        return options;
    }
}
