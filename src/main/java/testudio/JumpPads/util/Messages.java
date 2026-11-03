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
            return "";
        }
    }
}
