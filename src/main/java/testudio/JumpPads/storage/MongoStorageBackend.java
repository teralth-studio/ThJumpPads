package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MongoStorageBackend implements StorageBackend {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoStorageBackend(JumpPadsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String uri = required(config.getString("storage.mongodb.uri"), "storage.mongodb.uri");
        String database = required(config.getString("storage.mongodb.database"), "storage.mongodb.database");
        String collectionName = required(config.getString("storage.mongodb.collection"),
                "storage.mongodb.collection");
        MongoClient opened = null;
        MongoCollection<Document> selectedCollection;
        try {
            opened = MongoClients.create(uri);
            MongoDatabase mongoDatabase = opened.getDatabase(database);
            selectedCollection = mongoDatabase.getCollection(collectionName);
            selectedCollection.createIndex(
                    Indexes.ascending("nameKey"),
                    new IndexOptions()
                            .unique(true)
                            .partialFilterExpression(new Document("nameKey", new Document("$type", "string"))));
        } catch (MongoException | IllegalArgumentException e) {
            if (opened != null) {
                opened.close();
            }
            throw new StorageException("No se pudo abrir la base de datos MongoDB.", e);
        }
        client = opened;
        collection = selectedCollection;
    }

    @Override
    public List<StoredToggle> load() {
        List<StoredToggle> states = new ArrayList<>();
        try {
            for (Document document : collection.find()) {
                String rawUuid = document.getString("uuid");
                String nameKey = document.getString("nameKey");
                Boolean enabled = document.getBoolean("enabled");
                if (enabled == null || (rawUuid == null && nameKey == null)) {
                    throw new StorageException("MongoDB contiene un estado de jugador incompleto.");
                }
                UUID uuid = rawUuid == null ? null : UUID.fromString(rawUuid);
                states.add(new StoredToggle(uuid, nameKey, enabled));
            }
            return states;
        } catch (MongoException | IllegalArgumentException e) {
            throw new StorageException("No se pudieron cargar los estados de jugadores desde MongoDB.", e);
        }
    }

    @Override
    public void save(UUID uuid, String nameKey, boolean enabled) {
        try {
            Document replacement = new Document("nameKey", nameKey)
                    .append("uuid", uuid.toString())
                    .append("enabled", enabled);
            if (collection.replaceOne(Filters.eq("nameKey", nameKey), replacement).getMatchedCount() == 0
                    && collection.replaceOne(Filters.eq("uuid", uuid.toString()), replacement)
                    .getMatchedCount() == 0) {
                collection.insertOne(replacement);
            }
        } catch (MongoException e) {
            throw new StorageException("No se pudo guardar el estado del jugador " + uuid + " en MongoDB.", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private static String required(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new StorageException(path + " no puede estar vacio.");
        }
        return value.trim();
    }
}
