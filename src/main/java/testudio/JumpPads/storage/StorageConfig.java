package testudio.JumpPads.storage;

import org.bukkit.configuration.file.FileConfiguration;

final class StorageConfig {

    final String type;
    final String sqliteFile;
    final String mysqlHost;
    final int mysqlPort;
    final String mysqlDatabase;
    final String mysqlUsername;
    final String mysqlPassword;
    final boolean mysqlUseSsl;
    final boolean mysqlVerifyServerCertificate;
    final boolean mysqlAllowPublicKeyRetrieval;
    final int mysqlConnectionTimeout;

    private StorageConfig(FileConfiguration config) {
        type = config.getString("storage.type", "sqlite");
        sqliteFile = config.getString("storage.sqlite.file");
        mysqlHost = config.getString("storage.mysql.host");
        mysqlPort = config.getInt("storage.mysql.port", 3306);
        mysqlDatabase = config.getString("storage.mysql.database");
        mysqlUsername = config.getString("storage.mysql.username", "root");
        mysqlPassword = config.getString("storage.mysql.password", "");
        mysqlUseSsl = config.getBoolean("storage.mysql.use-ssl", true);
        mysqlVerifyServerCertificate = config.getBoolean("storage.mysql.verify-server-certificate", true);
        mysqlAllowPublicKeyRetrieval = config.getBoolean("storage.mysql.allow-public-key-retrieval", false);
        mysqlConnectionTimeout = config.getInt("storage.mysql.connection-timeout", 10000);
    }

    static StorageConfig capture(FileConfiguration config) {
        return new StorageConfig(config);
    }
}
