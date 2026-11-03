package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;
import testudio.JumpPads.util.ConsoleMessages;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ToggleStorage {

    private final JumpPadsPlugin plugin;
    private final Map<String, Boolean> statesByName = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> legacyStatesByUuid = new ConcurrentHashMap<>();
    private final ExecutorService writes = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "JumpPads-Storage");
        thread.setDaemon(true);
        return thread;
    });

    private volatile StorageBackend backend;

    public ToggleStorage(JumpPadsPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load() {
        drainWrites();
        StorageBackend nextBackend = createBackend();
        List<StoredToggle> loaded;
        try {
            loaded = nextBackend.load();
            importLegacyDisabledPlayers(loaded);
        } catch (StorageException e) {
            closeAfterFailure(nextBackend, e);
            throw e;
        }

        StorageBackend previous = backend;
        if (previous != null) {
            try {
                previous.close();
            } catch (StorageException e) {
                closeAfterFailure(nextBackend, e);
                throw e;
            }
        }

        statesByName.clear();
        legacyStatesByUuid.clear();
        for (StoredToggle state : loaded) {
            if (state.nameKey() != null) {
                statesByName.put(state.nameKey(), state.enabled());
            } else if (state.uuid() != null) {
                legacyStatesByUuid.put(state.uuid(), state.enabled());
            }
        }
        backend = nextBackend;
    }

    public synchronized boolean isEnabled(UUID uuid, String playerName) {
        String nameKey = nameKey(playerName);
        if (nameKey == null) {
            return legacyStatesByUuid.getOrDefault(uuid, true);
        }
        Boolean namedState = statesByName.get(nameKey);
        boolean enabled = namedState != null
                ? namedState
                : legacyStatesByUuid.getOrDefault(uuid, true);
        if (namedState == null) {
            statesByName.put(nameKey, enabled);
            boolean wasLegacyState = legacyStatesByUuid.remove(uuid) != null;
            if (wasLegacyState) {
                persistState(uuid, nameKey, enabled);
            }
        }
        return enabled;
    }

    public synchronized void bind(UUID uuid, String playerName) {
        String nameKey = nameKey(playerName);
        if (nameKey != null) {
            isEnabled(uuid, playerName);
        }
    }

    public synchronized void setEnabled(UUID uuid, String playerName, boolean enabled) {
        String nameKey = nameKey(playerName);
        if (nameKey == null) {
            throw new StorageException("No se pudo determinar el nombre del jugador.");
        }
        StorageBackend current = backend;
        if (current == null) {
            throw new StorageException("El almacenamiento aun no esta inicializado.");
        }
        statesByName.put(nameKey, enabled);
        legacyStatesByUuid.remove(uuid);
        writes.execute(() -> {
            try {
                current.save(uuid, nameKey, enabled);
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not persist player state for " + playerName
                        + ": " + e.detailedMessage());
            }
        });
    }

    public synchronized boolean toggle(UUID uuid, String playerName) {
        boolean next = !isEnabled(uuid, playerName);
        setEnabled(uuid, playerName, next);
        return next;
    }

    public synchronized void save() {
        try {
            drainWrites();
        } catch (StorageException e) {
            ConsoleMessages.error(plugin, "Could not complete all pending storage writes: "
                    + e.detailedMessage());
        }

        StorageBackend current = backend;
        backend = null;
        if (current != null) {
            try {
                current.close();
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not close storage: " + e.detailedMessage());
            }
        }
        writes.shutdown();
    }

    private StorageBackend createBackend() {
        String configuredType = plugin.getConfig().getString("storage.type", "sqlite");
        String type = configuredType == null ? "" : configuredType.trim().toLowerCase(Locale.ROOT);
        if (type.equals("sqlite")) {
            return new SqlStorageBackend(plugin, false);
        }
        if (type.equals("mysql")) {
            return new SqlStorageBackend(plugin, true);
        }
        if (type.equals("mongodb")) {
            return new MongoStorageBackend(plugin);
        }
        throw new StorageException("Tipo de almacenamiento no valido: " + type
                + ". Usa sqlite, mysql o mongodb.");
    }

    private void importLegacyDisabledPlayers(List<StoredToggle> loaded) {
        File legacyFile = new File(plugin.getDataFolder(), "players.yml");
        if (!legacyFile.isFile()) {
            return;
        }

        List<String> disabledPlayers = YamlConfiguration.loadConfiguration(legacyFile)
                .getStringList("disabled");
        for (String raw : disabledPlayers) {
            try {
                loaded.add(new StoredToggle(UUID.fromString(raw), null, false));
            } catch (IllegalArgumentException e) {
                ConsoleMessages.warning(plugin, "Invalid UUID in players.yml: " + raw);
            }
        }
    }

    private void persistState(UUID uuid, String nameKey, boolean enabled) {
        StorageBackend current = backend;
        if (current == null) {
            throw new StorageException("El almacenamiento aun no esta inicializado.");
        }
        writes.execute(() -> {
            try {
                current.save(uuid, nameKey, enabled);
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not migrate player state for " + uuid
                        + ": " + e.detailedMessage());
            }
        });
    }

    private void drainWrites() {
        Future<?> barrier = writes.submit(() -> {
        });
        try {
            barrier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Se interrumpio la espera de escrituras pendientes.", e);
        } catch (ExecutionException e) {
            throw new StorageException("Fallo una escritura pendiente de almacenamiento.", e.getCause());
        }
    }

    private void closeAfterFailure(StorageBackend candidate, StorageException failure) {
        try {
            candidate.close();
        } catch (StorageException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static String nameKey(String playerName) {
        return playerName == null || playerName.trim().isEmpty()
                ? null
                : playerName.trim().toLowerCase(Locale.ROOT);
    }
}
