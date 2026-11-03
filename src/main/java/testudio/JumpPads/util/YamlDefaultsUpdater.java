package testudio.JumpPads.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlDefaultsUpdater {

    private YamlDefaultsUpdater() {
    }

    public static void appendMissingKeys(JavaPlugin plugin, File file, String resourceName) {
        YamlConfiguration defaults = loadDefaults(plugin, resourceName);
        appendMissingKeys(file, defaults);
    }

    static void appendMissingKeys(File file, YamlConfiguration defaults) {
        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        String content;
        try {
            content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + file.getName() + ".", e);
        }

        String[] sourceLines = content.split("\\r?\\n", -1);
        Map<String, List<String>> additions = new LinkedHashMap<String, List<String>>();
        collectMissing(defaults, current, "", sourceLines, additions);
        if (additions.isEmpty()) {
            return;
        }

        String lineEnding = content.contains("\r\n") ? "\r\n" : "\n";
        List<Map.Entry<String, List<String>>> orderedAdditions =
                new ArrayList<Map.Entry<String, List<String>>>(additions.entrySet());
        Collections.sort(orderedAdditions, new Comparator<Map.Entry<String, List<String>>>() {
            @Override
            public int compare(Map.Entry<String, List<String>> left, Map.Entry<String, List<String>> right) {
                return Integer.compare(pathDepth(right.getKey()), pathDepth(left.getKey()));
            }
        });
        List<String> updated = new ArrayList<String>(java.util.Arrays.asList(sourceLines));
        for (Map.Entry<String, List<String>> addition : orderedAdditions) {
            String[] currentLines = updated.toArray(new String[0]);
            int parentIndex = findSection(currentLines, addition.getKey());
            int parentIndent = parentIndex < 0 ? -2 : indentation(currentLines[parentIndex]);
            int insertionIndex = parentIndex < 0
                    ? updated.size() - (updated.get(updated.size() - 1).isEmpty() ? 1 : 0)
                    : sectionEnd(currentLines, parentIndex, parentIndent);
            updated.addAll(insertionIndex, addition.getValue());
        }

        try {
            Files.write(file.toPath(), join(updated, lineEnding).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron agregar las claves faltantes a "
                    + file.getName() + ".", e);
        }
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin, String resourceName) {
        InputStream resource = plugin.getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("No se encontro el recurso predeterminado " + resourceName + ".");
        }
        try (InputStream input = resource;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron leer los valores predeterminados de "
                    + resourceName + ".", e);
        }
    }

    private static void collectMissing(ConfigurationSection defaults, ConfigurationSection current,
                                       String parentPath, String[] lines,
                                       Map<String, List<String>> additions) {
        for (String key : defaults.getKeys(false)) {
            String path = parentPath.isEmpty() ? key : parentPath + "." + key;
            if (current.contains(key)) {
                ConfigurationSection defaultChild = defaults.getConfigurationSection(key);
                ConfigurationSection currentChild = current.getConfigurationSection(key);
                if (defaultChild != null && currentChild != null) {
                    collectMissing(defaultChild, currentChild, path, lines, additions);
                }
                continue;
            }

            List<String> renderedLines = additions.get(parentPath);
            if (renderedLines == null) {
                renderedLines = new ArrayList<String>();
                additions.put(parentPath, renderedLines);
            }
            int parentIndex = findSection(lines, parentPath);
            int indent = parentPath.isEmpty() ? 0 : indentation(lines[parentIndex]) + 2;
            appendEntry(renderedLines, key, defaults.get(key), indent);
        }
    }

    private static void appendEntry(List<String> lines, String key, Object value, int indent) {
        String prefix = spaces(indent);
        ConfigurationSection section = value instanceof ConfigurationSection
                ? (ConfigurationSection) value
                : null;
        if (section != null) {
            lines.add(prefix + key + ":");
            for (String child : section.getKeys(false)) {
                appendEntry(lines, child, section.get(child), indent + 2);
            }
            return;
        }
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            lines.add(prefix + key + ":");
            for (Object item : (List<?>) value) {
                lines.add(spaces(indent + 2) + "-" + serializedValue(item));
            }
            return;
        }
        lines.add(prefix + key + ":" + serializedValue(value));
    }

    private static String serializedValue(Object value) {
        YamlConfiguration scalar = new YamlConfiguration();
        scalar.set("value", value);
        String serialized = scalar.saveToString();
        int separator = serialized.indexOf(':');
        int newline = serialized.indexOf('\n', separator);
        return serialized.substring(separator + 1, newline < 0 ? serialized.length() : newline);
    }

    private static int findSection(String[] lines, String path) {
        if (path.isEmpty()) {
            return -1;
        }
        String[] parts = path.split("\\.");
        int parentIndex = -1;
        int parentIndent = -2;
        for (String part : parts) {
            parentIndex = findChild(lines, parentIndex, parentIndent, part);
            if (parentIndex < 0) {
                throw new IllegalStateException("No se encontro la seccion YAML existente: " + path);
            }
            parentIndent = indentation(lines[parentIndex]);
        }
        return parentIndex;
    }

    private static int findChild(String[] lines, int parentIndex, int parentIndent, String key) {
        int start = parentIndex < 0 ? 0 : parentIndex + 1;
        int directIndent = Integer.MAX_VALUE;
        int match = -1;
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            int indent = indentation(line);
            if (parentIndex >= 0 && indent <= parentIndent) {
                break;
            }
            if (indent < directIndent) {
                directIndent = indent;
                match = -1;
            }
            if (indent == directIndent && keyFromLine(line).equals(key)) {
                match = i;
            }
        }
        return match;
    }

    private static int sectionEnd(String[] lines, int sectionIndex, int sectionIndent) {
        for (int i = sectionIndex + 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            if (indentation(line) <= sectionIndent) {
                int insertionIndex = i;
                while (insertionIndex > sectionIndex + 1) {
                    String previous = lines[insertionIndex - 1].trim();
                    if (!previous.isEmpty() && !previous.startsWith("#")) {
                        break;
                    }
                    insertionIndex--;
                }
                return insertionIndex;
            }
        }
        return lines.length - (lines[lines.length - 1].isEmpty() ? 1 : 0);
    }

    private static String keyFromLine(String line) {
        String trimmed = line.trim();
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return "";
        }
        String key = trimmed.substring(0, separator).trim();
        if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1);
        }
        if (key.length() >= 2 && key.startsWith("'") && key.endsWith("'")) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    private static int indentation(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static int pathDepth(String path) {
        return path.isEmpty() ? 0 : path.split("\\.").length;
    }

    private static String spaces(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String join(List<String> lines, String lineEnding) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append(lineEnding);
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }
}
