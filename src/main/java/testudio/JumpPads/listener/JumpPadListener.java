package testudio.JumpPads.listener;

import testudio.JumpPads.JumpPadsPlugin;
import testudio.JumpPads.config.Settings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public final class JumpPadListener implements Listener {

    private final JumpPadsPlugin plugin;

    public JumpPadListener(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.toggles().bind(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        tryJump(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        tryJump(event.getPlayer(), event.getPlayer().getLocation());
    }

    private void tryJump(Player player, Location location) {
        Settings settings = plugin.settings();

        if (!settings.isWorldEnabled(player.getWorld())) {
            return;
        }
        if (!plugin.toggles().isEnabled(player.getUniqueId(), player.getName())) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying()) {
            return;
        }

        Block pad = padAt(location);
        if (pad == null) {
            return;
        }

        if (settings.requiresBlockBelow()
                && pad.getRelative(0, -1, 0).getType() != settings.blockBelow()) {
            return;
        }

        launch(player, settings);
    }

    /**
     * Busca el bloque del pad en la posicion de los pies, y tambien un bloque
     * mas abajo (placas/alfombras tienen hitbox baja y el jugador puede quedar
     * registrado en el bloque superior).
     */
    private Block padAt(Location location) {
        Settings settings = plugin.settings();
        Block feet = location.getBlock();
        if (feet.getType() == settings.jumpBlock()) {
            return feet;
        }
        Block below = feet.getRelative(0, -1, 0);
        if (below.getType() == settings.jumpBlock()) {
            return below;
        }
        return null;
    }

    private void launch(Player player, Settings settings) {
        float yaw = player.getLocation().getYaw();
        double radians = Math.toRadians(yaw);
        Vector direction = new Vector(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vector velocity = direction.multiply(settings.jumpWidthVelocity())
                .setY(settings.jumpHeightVelocity());

        player.setVelocity(velocity);
        player.setFallDistance(0.0F);

        if (settings.soundEnabled()) {
            player.playSound(player.getLocation(), settings.sound(), settings.soundVolume(), settings.soundPitch());
        }
    }
}
