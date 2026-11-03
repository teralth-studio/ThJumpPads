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
import java.util.function.Consumer;

public final class ToggleStorage {

    private final JumpPadsPlugin plugin;
    private final PluginLibraryManager libraries;
    private final Map<String, Boolean> statesByName = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> legacyStatesByUuid = new ConcurrentHashMap<>();
    private final ExecutorService writes = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "JumpPads-Storage");
        thread.setDaemon(true);
        return thread;
    });

    private volatile StorageBackend backend;
    private volatile boolean ready;
    private boolean closed;
    private boolean initialLoadInProgress;
    private StorageBackend pendingBackend;

    public ToggleStorage(JumpPadsPlugin plugin) {
        this.plugin = plugin;
        this.libraries = new PluginLibraryManager(plugin);
    }

    public synchronized void load() {
        drainWrites();
        StorageConfig config = StorageConfig.capture(plugin.getConfig());
        List<StoredToggle> legacyStates = readLegacyDisabledPlayers();
        StorageBackend nextBackend = createBackend(config);
        List<StoredToggle> loaded;
        try {
            loaded = nextBackend.load();
            loaded.addAll(legacyStates);
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
        ready = true;
    }

    public void loadAsync(Consumer<StorageException> completion) {
        StorageConfig config = StorageConfig.capture(plugin.getConfig());
        List<StoredToggle> legacyStates = readLegacyDisabledPlayers();
        synchronized (this) {
            if (closed) {
                completion.accept(new StorageException("Storage has been closed."));
                return;
            }
            if (initialLoadInProgress || backend != null) {
                completion.accept(new StorageException("Storage initialization is already in progress."));
                return;
            }
            initialLoadInProgress = true;
        }

        try {
            writes.execute(() -> initializeBackendAsync(config, legacyStates, completion));
        } catch (RuntimeException e) {
            synchronized (this) {
                initialLoadInProgress = false;
            }
            completion.accept(new StorageException("Could not start asynchronous storage initialization.", e));
        }
    }

    public boolean isReady() {
        return ready;
    }

    public synchronized boolean isEnabled(UUID uuid, String playerName) {
        if (!ready) {
            return true;
        }
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
        if (!ready) {
            return;
        }
        String nameKey = nameKey(playerName);
        if (nameKey != null) {
            isEnabled(uuid, playerName);
        }
    }

    public synchronized void setEnabled(UUID uuid, String playerName, boolean enabled) {
        String nameKey = nameKey(playerName);
        if (nameKey == null) {
            throw new StorageException("Could not determine the player name.");
        }
        StorageBackend current = backend;
        if (current == null) {
            throw new StorageException("Storage has not been initialized.");
        }
        statesByName.put(nameKey, enabled);
        legacyStatesByUuid.remove(uuid);
        writes.execute(() -> {
            try {
                current.save(uuid, nameKey, enabled);
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not persist player state for &b" + playerName
                        + "&c: &b" + e.detailedMessage());
            }
        });
    }

    public synchronized boolean toggle(UUID uuid, String playerName) {
        boolean next = !isEnabled(uuid, playerName);
        setEnabled(uuid, playerName, next);
        return next;
    }

    public synchronized void save() {
        if (closed) {
            return;
        }
        closed = true;
        ready = false;
        StorageBackend loading = pendingBackend;
        pendingBackend = null;
        boolean initializing = initialLoadInProgress;

        try {
            if (backend != null) {
                drainWrites();
            }
        } catch (StorageException e) {
            ConsoleMessages.error(plugin, "Could not complete all pending storage writes: &b"
                    + e.detailedMessage());
        }

        StorageBackend current = backend;
        backend = null;
        if (current != null) {
            try {
                current.close();
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not close storage: &b" + e.detailedMessage());
            }
        }
        if (loading != null) {
            closeAfterFailure(loading, new StorageException("Storage was closed during initialization."));
        }
        if (initializing) {
            writes.shutdownNow();
        } else {
            writes.shutdown();
            closeLibraries();
        }
    }

    private StorageBackend createBackend(StorageConfig config) {
        String type = config.type == null ? "" : config.type.trim().toLowerCase(Locale.ROOT);
        if (type.equals("sqlite")) {
            return new SqlStorageBackend(plugin, config, false, libraries.classLoaderFor(type));
        }
        if (type.equals("mysql")) {
            return new SqlStorageBackend(plugin, config, true, libraries.classLoaderFor(type));
        }
        if (type.equals("mongodb")) {
            return new MongoStorageBackend(config, libraries.classLoaderFor(type));
        }
        throw new StorageException("Invalid storage type: " + type + ". Use sqlite, mysql, or mongodb.");
    }

    private void initializeBackendAsync(StorageConfig config, List<StoredToggle> legacyStates,
                                        Consumer<StorageException> completion) {
        StorageBackend candidate = null;
        List<StoredToggle> loaded = null;
        StorageException failure = null;
        try {
            candidate = createBackend(config);
            loaded = candidate.load();
            loaded.addAll(legacyStates);
        } catch (StorageException e) {
            failure = e;
        } catch (RuntimeException | LinkageError e) {
            failure = new StorageException("Could not initialize the storage backend.", e);
        }

        final StorageBackend initializedBackend = candidate;
        final List<StoredToggle> initializedStates = loaded;
        final StorageException initializationFailure = failure;
        synchronized (this) {
            initialLoadInProgress = false;
            if (closed) {
                if (initializedBackend != null) {
                    closeAfterFailure(initializedBackend,
                            new StorageException("Storage was closed during initialization."));
                }
                closeLibraries();
                return;
            }
            pendingBackend = initializedBackend;
        }

        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                synchronized (ToggleStorage.this) {
                    if (closed) {
                        if (pendingBackend != null) {
                            closeAfterFailure(pendingBackend,
                                    new StorageException("Storage was closed during initialization."));
                            pendingBackend = null;
                        }
                        closeLibraries();
                        return;
                    }
                    if (initializationFailure != null) {
                        if (pendingBackend != null) {
                            closeAfterFailure(pendingBackend, initializationFailure);
                            pendingBackend = null;
                        }
                    } else {
                        applyLoadedStates(initializedStates);
                        backend = pendingBackend;
                        pendingBackend = null;
                        ready = true;
                    }
                }
                completion.accept(initializationFailure);
            });
        } catch (RuntimeException e) {
            synchronized (this) {
                initialLoadInProgress = false;
                if (pendingBackend != null) {
                    closeAfterFailure(pendingBackend, new StorageException(
                            "Could not schedule storage initialization completion.", e));
                    pendingBackend = null;
                }
                if (closed) {
                    closeLibraries();
                }
            }
        }
    }

    private void applyLoadedStates(List<StoredToggle> loaded) {
        statesByName.clear();
        legacyStatesByUuid.clear();
        for (StoredToggle state : loaded) {
            if (state.nameKey() != null) {
                statesByName.put(state.nameKey(), state.enabled());
            } else if (state.uuid() != null) {
                legacyStatesByUuid.put(state.uuid(), state.enabled());
            }
        }
    }

    private List<StoredToggle> readLegacyDisabledPlayers() {
        File legacyFile = new File(plugin.getDataFolder(), "players.yml");
        if (!legacyFile.isFile()) {
            return java.util.Collections.emptyList();
        }
        List<String> disabledPlayers = YamlConfiguration.loadConfiguration(legacyFile)
                .getStringList("disabled");
        List<StoredToggle> states = new java.util.ArrayList<StoredToggle>(disabledPlayers.size());
        for (String raw : disabledPlayers) {
            try {
                states.add(new StoredToggle(UUID.fromString(raw), null, false));
            } catch (IllegalArgumentException e) {
                ConsoleMessages.warning(plugin, "Invalid UUID in players.yml: &b" + raw);
            }
        }
        return states;
    }

    private void closeLibraries() {
        try {
            libraries.close();
        } catch (StorageException e) {
            ConsoleMessages.error(plugin, "Could not close storage libraries: &b" + e.detailedMessage());
        }
    }

    private void persistState(UUID uuid, String nameKey, boolean enabled) {
        StorageBackend current = backend;
        if (current == null) {
            throw new StorageException("Storage has not been initialized.");
        }
        writes.execute(() -> {
            try {
                current.save(uuid, nameKey, enabled);
            } catch (StorageException e) {
                ConsoleMessages.error(plugin, "Could not migrate player state for &b" + uuid
                        + "&c: &b" + e.detailedMessage());
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
            throw new StorageException("Waiting for pending storage writes was interrupted.", e);
        } catch (ExecutionException e) {
            throw new StorageException("A pending storage write failed.", e.getCause());
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
