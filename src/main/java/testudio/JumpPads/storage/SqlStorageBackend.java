package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
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

    public SqlStorageBackend(JumpPadsPlugin plugin, StorageConfig config, boolean mysql,
                             ClassLoader driverClassLoader) {
        Connection opened = null;
        String saveStatement = null;
        try {
            if (mysql) {
                String host = required(config.mysqlHost, "storage.mysql.host");
                if (!host.matches("[A-Za-z0-9.:-]+")) {
                    throw new StorageException("storage.mysql.host contains invalid characters.");
                }
                int port = config.mysqlPort;
                if (port < 1 || port > 65535) {
                    throw new StorageException("storage.mysql.port must be between 1 and 65535.");
                }
                String database = required(config.mysqlDatabase, "storage.mysql.database");
                if (!database.matches("[A-Za-z0-9_]+")) {
                    throw new StorageException(
                            "storage.mysql.database may only contain letters, numbers, and underscores.");
                }
                int timeout = config.mysqlConnectionTimeout;
                if (timeout < 1) {
                    throw new StorageException("storage.mysql.connection-timeout must be greater than zero.");
                }
                boolean useSsl = config.mysqlUseSsl;
                boolean verifyServerCertificate = config.mysqlVerifyServerCertificate;
                String sslMode = !useSsl
                        ? "DISABLED"
                        : verifyServerCertificate ? "VERIFY_IDENTITY" : "REQUIRED";

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?sslMode=" + sslMode
                        + "&allowPublicKeyRetrieval="
                        + config.mysqlAllowPublicKeyRetrieval
                        + "&connectTimeout=" + timeout;
                Properties properties = new Properties();
                properties.setProperty("user", required(config.mysqlUsername, "storage.mysql.username"));
                String password = config.mysqlPassword;
                properties.setProperty("password", password == null ? "" : password);
                opened = connect("com.mysql.cj.jdbc.Driver", driverClassLoader, url, properties);
                saveStatement = "INSERT INTO " + NAME_TABLE + " (name_key, uuid, enabled) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), enabled = VALUES(enabled)";
            } else {
                String configuredFile = required(config.sqliteFile, "storage.sqlite.file");
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
                opened = connect("org.sqlite.JDBC", driverClassLoader, jdbcUrl, new Properties());
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
            StorageException failure = new StorageException("Could not open the SQL database.", e);
            closeAfterFailure(opened, failure);
            throw failure;
        } catch (ReflectiveOperationException e) {
            StorageException failure = new StorageException("Could not load the SQL driver.", e);
            closeAfterFailure(opened, failure);
            throw failure;
        } catch (StorageException e) {
            closeAfterFailure(opened, e);
            throw e;
        }
        connection = opened;
        upsertSql = saveStatement;
    }

    private static Connection connect(String driverName, ClassLoader classLoader, String url,
                                      Properties properties)
            throws ReflectiveOperationException, SQLException {
        Class<?> driverClass = Class.forName(driverName, true, classLoader);
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new SQLException("The JDBC driver rejected the configured connection URL.");
        }
        return connection;
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
            throw new StorageException("Could not load player states by name.", e);
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
            throw new StorageException("Could not load player states.", e);
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
            throw new StorageException("Could not save player state for " + uuid + ".", e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new StorageException("Could not close the SQL connection.", e);
        }
    }

    private static void closeAfterFailure(Connection connection, StorageException failure) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    private static String required(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new StorageException(path + " cannot be empty.");
        }
        return value.trim();
    }
}
