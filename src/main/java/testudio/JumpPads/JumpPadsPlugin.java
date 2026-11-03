package testudio.JumpPads;

import testudio.JumpPads.command.JumpPadsCommand;
import testudio.JumpPads.config.Settings;
import testudio.JumpPads.hook.JumpPadsExpansion;
import testudio.JumpPads.listener.JumpPadListener;
import testudio.JumpPads.storage.ToggleStorage;
import testudio.JumpPads.storage.StorageException;
import testudio.JumpPads.util.ConsoleMessages;
import testudio.JumpPads.util.Messages;
import testudio.JumpPads.util.YamlDefaultsUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class JumpPadsPlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private ToggleStorage toggleStorage;
    private JumpPadsExpansion expansion;

    @Override
    public void onEnable() {
        ConsoleMessages.startup(this);
        startMetrics();

        saveDefaultConfig();

        this.settings = new Settings(this);
        this.messages = new Messages(this);
        this.toggleStorage = new ToggleStorage(this);

        try {
            reloadConfiguration();
        } catch (RuntimeException e) {
            ConsoleMessages.error(this, "Configuration initialization failed: &b" + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new JumpPadListener(this), this);

        PluginCommand command = getCommand("jumppads");
        if (command != null) {
            JumpPadsCommand executor = new JumpPadsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.expansion = new JumpPadsExpansion(this);
            this.expansion.register();
            ConsoleMessages.info(this, "Expansions detected: &bPlaceholderAPI");
        }

        ConsoleMessages.info(this, "Storage connection started in the background.");
        toggleStorage.loadAsync(failure -> {
            if (failure != null) {
                ConsoleMessages.error(this, "Storage initialization failed: &b" + failure.detailedMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            ConsoleMessages.initialized(this);
        });
    }

    @Override
    public void onDisable() {
        if (toggleStorage != null) {
            toggleStorage.save();
        }
        if (expansion != null) {
            expansion.unregister();
        }
        ConsoleMessages.disabled(this);
    }

    public void reloadAll() {
        reloadConfiguration();
        toggleStorage.load();
    }

    private void reloadConfiguration() {
        YamlDefaultsUpdater.appendMissingKeys(
                this, new File(getDataFolder(), "config.yml"), "config.yml");
        reloadConfig();
        settings.load();
        messages.load();
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public ToggleStorage toggles() {
        return toggleStorage;
    }

    private void startMetrics() {
        try {
            new Metrics(this, 35094);
        } catch (RuntimeException | LinkageError e) {
            ConsoleMessages.warning(this, "Could not initialize bStats: &b" + e.getMessage());
        }
    }
}
