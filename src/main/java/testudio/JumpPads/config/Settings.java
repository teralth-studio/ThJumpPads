package testudio.JumpPads.config;

import testudio.JumpPads.JumpPadsPlugin;
import testudio.JumpPads.util.ConsoleMessages;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Settings {

    private static final String[][] MATERIAL_ALIASES = {
            {"IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE"},
            {"GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE"},
            {"STONE_PLATE", "STONE_PRESSURE_PLATE"},
            {"WOOD_PLATE", "WOODEN_PRESSURE_PLATE", "OAK_PRESSURE_PLATE"},
            {"GRASS", "GRASS_BLOCK"},
            {"GRASS_PATH", "DIRT_PATH"},
            {"WOOD", "OAK_PLANKS"},
            {"LOG", "OAK_LOG"},
            {"LEAVES", "OAK_LEAVES"},
            {"WORKBENCH", "CRAFTING_TABLE"},
            {"ENDER_STONE", "END_STONE"},
            {"NETHER_BRICK", "NETHER_BRICKS"},
            {"HARD_CLAY", "TERRACOTTA"},
            {"THIN_GLASS", "GLASS_PANE"},
            {"IRON_FENCE", "IRON_BARS"},
            {"FENCE", "OAK_FENCE"},
            {"FENCE_GATE", "OAK_FENCE_GATE"},
            {"TRAP_DOOR", "OAK_TRAPDOOR"},
            {"WOOD_DOOR", "OAK_DOOR"},
            {"WOOD_BUTTON", "OAK_BUTTON"},
            {"WOOD_STEP", "OAK_SLAB"},
            {"STEP", "STONE_SLAB"}
    };

    private final JumpPadsPlugin plugin;

    private Material jumpBlock = legacyMaterial("IRON_PLATE");
    private boolean requiresBlockBelow = false;
    private Material blockBelow = Material.IRON_BLOCK;
    private int jumpHeight = 5;
    private int jumpWidth = 5;
    private double jumpHeightVelocity;
    private double jumpWidthVelocity;
    private List<String> enabledWorlds = Collections.emptyList();

    private boolean soundEnabled = true;
    private Sound sound = legacySound();
    private float soundVolume = 1.0f;
    private float soundPitch = 1.2f;

    public Settings(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();

        jumpBlock = material(c.getString("jump-block", "IRON_PLATE"), legacyMaterial("IRON_PLATE"), "jump-block");
        requiresBlockBelow = c.getBoolean("requires-block-below", false);
        blockBelow = material(c.getString("block-below", "IRON_BLOCK"), Material.IRON_BLOCK, "block-below");
        jumpHeight = Math.max(1, c.getInt("jump-height", 5));
        jumpWidth = Math.max(1, c.getInt("jump-width", 5));
        jumpHeightVelocity = velocityForHeight(jumpHeight);
        jumpWidthVelocity = velocityForWidth(jumpWidth, flightTicks(jumpHeightVelocity));
        enabledWorlds = c.getStringList("enabled-worlds");

        soundEnabled = c.getBoolean("sound.enabled", true);
        soundVolume = (float) c.getDouble("sound.volume", 1.0D);
        soundPitch = (float) c.getDouble("sound.pitch", 1.2D);
        String soundName = c.getString("sound.name", "LEVEL_UP");
        Sound resolved = resolveSound(soundName);
        if (resolved == null) {
            ConsoleMessages.warning(plugin, "Sound unavailable in this Minecraft version (sound.name): "
                    + soundName);
        }
        sound = resolved != null ? resolved : legacySound();
    }

    private Material material(String raw, Material fallback, String path) {
        Material found = resolveMaterial(raw);
        if (found == null) {
            ConsoleMessages.warning(plugin, "Invalid or unavailable material for this Minecraft version "
                    + "in config.yml (" + path + "): " + raw + ". Using " + fallback + ".");
            return fallback;
        }
        return found;
    }

    private static Material legacyMaterial(String name) {
        Material legacy = resolveMaterial(name);
        return legacy != null ? legacy : Material.values()[0];
    }

    private static Material resolveMaterial(String raw) {
        String normalized = normalizeMaterialName(raw);
        for (String[] aliases : MATERIAL_ALIASES) {
            for (String alias : aliases) {
                if (alias.equals(normalized)) {
                    for (String candidate : aliases) {
                        Material material = materialByName(candidate);
                        if (material != null) {
                            return material;
                        }
                    }
                    return null;
                }
            }
        }
        return materialByName(normalized);
    }

    private static String normalizeMaterialName(String raw) {
        String normalized = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        return normalized.replace(' ', '_').replace('-', '_');
    }

    private static Material materialByName(String name) {
        for (Material candidate : Material.values()) {
            if (candidate.name().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }

    private static Sound resolveSound(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT).replace('.', '_');
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            if (normalized.equals("LEVEL_UP")) {
                return soundByName("ENTITY_PLAYER_LEVELUP");
            }
            if (normalized.equals("ENTITY_PLAYER_LEVELUP")) {
                return soundByName("LEVEL_UP");
            }
            return null;
        }
    }

    private static Sound soundByName(String name) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Sound legacySound() {
        Sound sound = soundByName("LEVEL_UP");
        Sound modernSound = soundByName("ENTITY_PLAYER_LEVELUP");
        return sound != null ? sound : modernSound != null ? modernSound : Sound.values()[0];
    }

    public boolean isWorldEnabled(World world) {
        if (enabledWorlds == null || enabledWorlds.isEmpty()) {
            return true;
        }
        return enabledWorlds.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
    }

    public Material jumpBlock() {
        return jumpBlock;
    }

    public boolean requiresBlockBelow() {
        return requiresBlockBelow;
    }

    public Material blockBelow() {
        return blockBelow;
    }

    public int jumpHeight() {
        return jumpHeight;
    }

    public int jumpWidth() {
        return jumpWidth;
    }

    public double jumpHeightVelocity() {
        return jumpHeightVelocity;
    }

    public double jumpWidthVelocity() {
        return jumpWidthVelocity;
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public Sound sound() {
        return sound;
    }

    public float soundVolume() {
        return soundVolume;
    }

    public float soundPitch() {
        return soundPitch;
    }

    private static double velocityForHeight(int height) {
        double low = 0.0D;
        double high = Math.max(1.0D, height);
        for (int i = 0; i < 40; i++) {
            double candidate = (low + high) / 2.0D;
            if (apexHeight(candidate) < height) {
                low = candidate;
            } else {
                high = candidate;
            }
        }
        return high;
    }

    private static double apexHeight(double initialVelocity) {
        double position = 0.0D;
        double velocity = initialVelocity;
        for (int tick = 0; tick < 10000 && velocity > 0.0D; tick++) {
            position += velocity;
            velocity = (velocity - 0.08D) * 0.98D;
        }
        return position;
    }

    private static int flightTicks(double initialVelocity) {
        double position = 0.0D;
        double velocity = initialVelocity;
        for (int tick = 1; tick <= 10000; tick++) {
            position += velocity;
            velocity = (velocity - 0.08D) * 0.98D;
            if (tick > 1 && position <= 0.0D && velocity < 0.0D) {
                return tick;
            }
        }
        return 10000;
    }

    private static double velocityForWidth(int width, int ticks) {
        double distancePerUnitVelocity = 0.0D;
        double velocityFactor = 1.0D;
        for (int tick = 0; tick < ticks; tick++) {
            distancePerUnitVelocity += velocityFactor;
            velocityFactor *= 0.91D;
        }
        return width / distancePerUnitVelocity;
    }
}
