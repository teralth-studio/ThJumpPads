package testudio.JumpPads.util;

import testudio.JumpPads.JumpPadsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final JumpPadsPlugin plugin;
    private YamlConfiguration config;
    private String prefix = "";

    public Messages(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlDefaultsUpdater.appendMissingKeys(plugin, file, "messages.yml");
        config = YamlConfiguration.loadConfiguration(file);
        prefix = config.getString("prefix", "");
    }

    public String raw(String key) {
        return config.getString(key, "&cMissing message: " + key);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.<String, String>emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = raw(key);
        if (message == null || message.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        message = prefix + message;
        if (sender instanceof Player) {
            message = PlaceholderSupport.apply((Player) sender, message);
        }
        sender.sendMessage(colorize(message));
    }

    public static String colorize(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer builder = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(hexColor(matcher.group(1))));
        }
        matcher.appendTail(builder);
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    private static String hexColor(String hex) {
        String color = "#" + hex;
        try {
            Class<?> chatColor = Class.forName("net.md_5.bungee.api.ChatColor");
            return (String) chatColor.getMethod("of", String.class).invoke(null, color).toString();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError e) {
            return nearestLegacyColor(hex);
        }
    }

    private static String nearestLegacyColor(String hex) {
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);
        int[][] palette = {
                {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
                {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
                {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
                {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}
        };
        ChatColor[] colors = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
                ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
                ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
                ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };
        int nearest = 0;
        int distance = Integer.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            int redDelta = red - palette[i][0];
            int greenDelta = green - palette[i][1];
            int blueDelta = blue - palette[i][2];
            int candidate = redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta;
            if (candidate < distance) {
                nearest = i;
                distance = candidate;
            }
        }
        return colors[nearest].toString();
    }
}
