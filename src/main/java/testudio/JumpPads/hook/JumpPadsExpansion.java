package testudio.JumpPads.hook;

import testudio.JumpPads.JumpPadsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class JumpPadsExpansion extends PlaceholderExpansion {

    private final JumpPadsPlugin plugin;

    public JumpPadsExpansion(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jumppads";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nejeh";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        if (params.equalsIgnoreCase("status")) {
            return plugin.toggles().isEnabled(player.getUniqueId(), player.getName())
                    ? "enabled"
                    : "disabled";
        }
        return null;
    }
}
