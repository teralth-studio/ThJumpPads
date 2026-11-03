package testudio.JumpPads.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderSupport {

    private static Boolean available;

    private PlaceholderSupport() {
    }

    public static String apply(Player player, String text) {
        if (available == null) {
            available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        }
        if (!available) {
            return text;
        }
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public static void reset() {
        available = null;
    }
}
