package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class SqlStorageBackend implements StorageBackend {

    private static final String TABLE = "jumppads_player_toggles";
    private static final String NAME_TABLE = "jumppads_player_toggles_by_name";

    private final Connection connection;
    private final String upsertSql;
    private final URLClassLoader sqliteDriverLoader;

    public SqlStorageBackend(JumpPadsPlugin plugin, boolean mysql) {
        FileConfiguration config = plugin.getConfig();
        Connection opened = null;
        String saveStatement = null;
        URLClassLoader openedDriverLoader = null;
        try {
            if (mysql) {
                String host = required(config.getString("storage.mysql.host"), "storage.mysql.host");
                if (!host.matches("[A-Za-z0-9.:-]+")) {
                    throw new StorageException("storage.mysql.host contiene caracteres no validos.");
                }
                int port = config.getInt("storage.mysql.port", 3306);
                if (port < 1 || port > 65535) {
                    throw new StorageException("storage.mysql.port debe estar entre 1 y 65535.");
                }
                String database = required(config.getString("storage.mysql.database"), "storage.mysql.database");
                if (!database.matches("[A-Za-z0-9_]+")) {
                    throw new StorageException("storage.mysql.database solo puede contener letras, numeros y guion bajo.");
                }
                int timeout = config.getInt("storage.mysql.connection-timeout", 10000);
                if (timeout < 1) {
                    throw new StorageException("storage.mysql.connection-timeout debe ser mayor que cero.");
                }

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=" + config.getBoolean("storage.mysql.use-ssl", false)
                        + "&verifyServerCertificate="
                        + config.getBoolean("storage.mysql.verify-server-certificate", true)
                        + "&allowPublicKeyRetrieval="
                        + config.getBoolean("storage.mysql.allow-public-key-retrieval", false)
                        + "&connectTimeout=" + timeout;
                Properties properties = new Properties();
                properties.setProperty("user",
                        required(config.getString("storage.mysql.username", "root"), "storage.mysql.username"));
                String password = config.getString("storage.mysql.password", "");
                properties.setProperty("password", password == null ? "" : password);
                opened = DriverManager.getConnection(url, properties);
                saveStatement = "INSERT INTO " + NAME_TABLE + " (name_key, uuid, enabled) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), enabled = VALUES(enabled)";
            } else {
                String configuredFile = required(config.getString("storage.sqlite.file"), "storage.sqlite.file");
                Path path = Paths.get(configuredFile);
                if (!path.isAbsolute()) {
                    path = plugin.getDataFolder().toPath().resolve(path);
                }
                path = path.normalize();
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                String jdbcUrl = "jdbc:sqlite:" + path;
                Path driverJar = new File(new File(plugin.getDataFolder(), "lib"),
                        "sqlite-jdbc-3.50.3.0.jar").toPath();
                if (Files.isRegularFile(driverJar)) {
                    URL driverUrl = driverJar.toUri().toURL();
                    openedDriverLoader = new URLClassLoader(
                            new URL[]{driverUrl}, SqlStorageBackend.class.getClassLoader());
                    Class<?> driverClass = Class.forName("org.sqlite.JDBC", true, openedDriverLoader);
                    Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                    opened = driver.connect(jdbcUrl, new Properties());
                    if (opened == null) {
                        throw new SQLException("El driver SQLite no acepta la URL de conexion configurada.");
                    }
                } else {
                    try {
                        Class.forName("org.sqlite.JDBC");
                    } catch (ClassNotFoundException e) {
                        throw new StorageException(
                                "Falta el driver SQLite. En Spigot 1.8.8, coloca sqlite-jdbc-3.50.3.0.jar "
                                        + "en plugins/ThJumpPads/lib/ o usa Paper, que descarga la dependencia.",
                                e);
                    }
                    opened = DriverManager.getConnection(jdbcUrl);
                }
                saveStatement = "INSERT INTO " + NAME_TABLE + " (name_key, uuid, enabled) VALUES (?, ?, ?) "
                        + "ON CONFLICT(name_key) DO UPDATE SET uuid = excluded.uuid, enabled = excluded.enabled";
            }
            try (Statement statement = opened.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE
                        + " (uuid VARCHAR(36) NOT NULL PRIMARY KEY, enabled BOOLEAN NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + NAME_TABLE
                        + " (name_key VARCHAR(64) NOT NULL PRIMARY KEY, uuid VARCHAR(36) NOT NULL, "
                        + "enabled BOOLEAN NOT NULL)");
            }
        } catch (SQLException | IOException e) {
            StorageException failure = new StorageException("No se pudo abrir la base de datos SQL.", e);
            closeAfterFailure(opened, openedDriverLoader, failure);
            throw failure;
        } catch (ReflectiveOperationException e) {
            StorageException failure = new StorageException(
                    "No se pudo cargar el driver SQLite desde plugins/ThJumpPads/lib/.", e);
            closeAfterFailure(opened, openedDriverLoader, failure);
            throw failure;
        } catch (StorageException e) {
            closeAfterFailure(opened, openedDriverLoader, e);
            throw e;
        }
        connection = opened;
        upsertSql = saveStatement;
        sqliteDriverLoader = openedDriverLoader;
    }

    @Override
    public List<StoredToggle> load() {
        List<StoredToggle> states = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT uuid, name_key, enabled FROM " + NAME_TABLE)) {
            while (result.next()) {
                states.add(new StoredToggle(
                        UUID.fromString(result.getString("uuid")),
                        result.getString("name_key"),
                        result.getBoolean("enabled")));
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new StorageException("No se pudieron cargar los estados por nombre.", e);
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT uuid, enabled FROM " + TABLE)) {
            while (result.next()) {
                states.add(new StoredToggle(
                        UUID.fromString(result.getString("uuid")),
                        null,
                        result.getBoolean("enabled")));
            }
            return states;
        } catch (SQLException | IllegalArgumentException e) {
            throw new StorageException("No se pudieron cargar los estados de jugadores.", e);
        }
    }

    @Override
    public void save(UUID uuid, String nameKey, boolean enabled) {
        try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setString(1, nameKey);
            statement.setString(2, uuid.toString());
            statement.setBoolean(3, enabled);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("No se pudo guardar el estado del jugador " + uuid + ".", e);
        }
    }

    @Override
    public void close() {
        StorageException failure = null;
        try {
            connection.close();
        } catch (SQLException e) {
            failure = new StorageException("No se pudo cerrar la conexion SQL.", e);
        }
        if (sqliteDriverLoader != null) {
            try {
                sqliteDriverLoader.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = new StorageException("No se pudo cerrar el cargador del driver SQLite.", e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void closeAfterFailure(Connection connection, URLClassLoader driverLoader,
                                          StorageException failure) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
        if (driverLoader != null) {
            try {
                driverLoader.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    private static String required(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new StorageException(path + " no puede estar vacio.");
        }
        return value.trim();
    }
}
