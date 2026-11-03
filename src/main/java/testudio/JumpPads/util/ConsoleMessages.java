package testudio.JumpPads.util;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConsoleMessages {

    private static final String PREFIX = "&9[ThJumpPads] ";
    private static final String SEPARATOR = "&8------------------------------------";

    private ConsoleMessages() {
    }

    public static void startup(JavaPlugin plugin) {
        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        send(console, SEPARATOR);
        send(console, "&9ThJumpPads &fby TeStudio");
        send(console, SEPARATOR);
        info(plugin, "Initializing plugin...");
    }

    public static void info(JavaPlugin plugin, String message) {
        send(plugin.getServer().getConsoleSender(), PREFIX + "&e" + message);
    }

    public static void warning(JavaPlugin plugin, String message) {
        send(plugin.getServer().getConsoleSender(), PREFIX + "&6" + message);
    }

    public static void error(JavaPlugin plugin, String message) {
        send(plugin.getServer().getConsoleSender(), PREFIX + "&c" + message);
    }

    public static void initialized(JavaPlugin plugin) {
        send(plugin.getServer().getConsoleSender(), PREFIX + "&aPlugin initialized!");
    }

    public static void disabled(JavaPlugin plugin) {
        send(plugin.getServer().getConsoleSender(), PREFIX + "&cPlugin disabled!");
    }

    private static void send(ConsoleCommandSender console, String message) {
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
