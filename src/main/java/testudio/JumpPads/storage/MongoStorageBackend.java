package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MongoStorageBackend implements StorageBackend {

    private final Object client;
    private final Object collection;
    private final Method closeClient;
    private final Method find;
    private final Method replaceOne;
    private final Method insertOne;
    private final Method filterByName;
    private final Method filterByUuid;
    private final Method documentGetString;
    private final Method documentGetBoolean;
    private final Constructor<?> documentConstructor;
    private final Method documentAppend;
    private final Method updateMatchedCount;

    public MongoStorageBackend(StorageConfig config, ClassLoader libraryClassLoader) {
        String uri = required(config.mongoUri, "storage.mongodb.uri");
        String databaseName = required(config.mongoDatabase, "storage.mongodb.database");
        String collectionName = required(config.mongoCollection, "storage.mongodb.collection");
        Object openedClient = null;
        try {
            Class<?> mongoClients = loadClass(libraryClassLoader, "com.mongodb.client.MongoClients");
            openedClient = mongoClients.getMethod("create", String.class).invoke(null, uri);

            Class<?> mongoClient = loadClass(libraryClassLoader, "com.mongodb.client.MongoClient");
            Object database = mongoClient.getMethod("getDatabase", String.class)
                    .invoke(openedClient, databaseName);
            Class<?> mongoDatabase = loadClass(libraryClassLoader, "com.mongodb.client.MongoDatabase");
            Object selectedCollection = mongoDatabase.getMethod("getCollection", String.class)
                    .invoke(database, collectionName);
            Class<?> mongoCollection = loadClass(libraryClassLoader, "com.mongodb.client.MongoCollection");
            Class<?> bson = loadClass(libraryClassLoader, "org.bson.conversions.Bson");
            Class<?> indexOptions = loadClass(libraryClassLoader, "com.mongodb.client.model.IndexOptions");
            Class<?> indexes = loadClass(libraryClassLoader, "com.mongodb.client.model.Indexes");
            Class<?> filters = loadClass(libraryClassLoader, "com.mongodb.client.model.Filters");
            Class<?> document = loadClass(libraryClassLoader, "org.bson.Document");
            Class<?> updateResult = loadClass(libraryClassLoader, "com.mongodb.client.result.UpdateResult");

            Object index = indexes.getMethod("ascending", String[].class)
                    .invoke(null, (Object) new String[]{"nameKey"});
            Object options = indexOptions.getDeclaredConstructor().newInstance();
            indexOptions.getMethod("unique", boolean.class).invoke(options, true);
            Object documentFilter = document.getDeclaredConstructor().newInstance();
            document.getMethod("append", String.class, Object.class)
                    .invoke(documentFilter, "$type", "string");
            Object partialFilter = document.getDeclaredConstructor().newInstance();
            document.getMethod("append", String.class, Object.class)
                    .invoke(partialFilter, "nameKey", documentFilter);
            indexOptions.getMethod("partialFilterExpression", bson).invoke(options, partialFilter);
            mongoCollection.getMethod("createIndex", bson, indexOptions)
                    .invoke(selectedCollection, index, options);

            client = openedClient;
            collection = selectedCollection;
            closeClient = mongoClient.getMethod("close");
            find = mongoCollection.getMethod("find");
            replaceOne = mongoCollection.getMethod("replaceOne", bson, Object.class);
            insertOne = mongoCollection.getMethod("insertOne", Object.class);
            filterByName = filters.getMethod("eq", String.class, Object.class);
            filterByUuid = filters.getMethod("eq", String.class, Object.class);
            documentGetString = document.getMethod("getString", Object.class);
            documentGetBoolean = document.getMethod("getBoolean", Object.class);
            documentConstructor = document.getDeclaredConstructor();
            documentAppend = document.getMethod("append", String.class, Object.class);
            updateMatchedCount = updateResult.getMethod("getMatchedCount");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            closeFailedClient(openedClient, e);
            throw new StorageException("Could not initialize the MongoDB driver.", e);
        }
    }

    @Override
    public List<StoredToggle> load() {
        List<StoredToggle> states = new ArrayList<StoredToggle>();
        try {
            Object results = find.invoke(collection);
            for (Object document : (Iterable<?>) results) {
                String rawUuid = (String) documentGetString.invoke(document, "uuid");
                String nameKey = (String) documentGetString.invoke(document, "nameKey");
                Boolean enabled = (Boolean) documentGetBoolean.invoke(document, "enabled");
                if (enabled == null || (rawUuid == null && nameKey == null)) {
                    throw new StorageException("MongoDB contains an incomplete player state.");
                }
                UUID uuid = rawUuid == null ? null : UUID.fromString(rawUuid);
                states.add(new StoredToggle(uuid, nameKey, enabled));
            }
            return states;
        } catch (StorageException e) {
            throw e;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new StorageException("Could not load player states from MongoDB.", causeOf(e));
        }
    }

    @Override
    public void save(UUID uuid, String nameKey, boolean enabled) {
        try {
            Object replacement = documentConstructor.newInstance();
            documentAppend.invoke(replacement, "nameKey", nameKey);
            documentAppend.invoke(replacement, "uuid", uuid.toString());
            documentAppend.invoke(replacement, "enabled", enabled);
            Object nameFilter = filterByName.invoke(null, "nameKey", nameKey);
            Object nameResult = replaceOne.invoke(collection, nameFilter, replacement);
            if (((Number) updateMatchedCount.invoke(nameResult)).longValue() == 0L) {
                Object uuidFilter = filterByUuid.invoke(null, "uuid", uuid.toString());
                Object uuidResult = replaceOne.invoke(collection, uuidFilter, replacement);
                if (((Number) updateMatchedCount.invoke(uuidResult)).longValue() == 0L) {
                    insertOne.invoke(collection, replacement);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new StorageException("Could not save player state for " + uuid + " in MongoDB.", causeOf(e));
        }
    }

    @Override
    public void close() {
        try {
            closeClient.invoke(client);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new StorageException("Could not close the MongoDB client.", causeOf(e));
        }
    }

    private static Class<?> loadClass(ClassLoader classLoader, String name) throws ClassNotFoundException {
        return Class.forName(name, true, classLoader);
    }

    private static Throwable causeOf(Exception exception) {
        if (exception instanceof InvocationTargetException
                && ((InvocationTargetException) exception).getCause() != null) {
            return ((InvocationTargetException) exception).getCause();
        }
        return exception;
    }

    private static Throwable causeOf(LinkageError error) {
        return error;
    }

    private static void closeFailedClient(Object client, Throwable failure) {
        if (client == null) {
            return;
        }
        try {
            client.getClass().getMethod("close").invoke(client);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError closeFailure) {
            failure.addSuppressed(causeOf(closeFailure));
        }
    }

    private static Throwable causeOf(Throwable throwable) {
        if (throwable instanceof InvocationTargetException
                && ((InvocationTargetException) throwable).getCause() != null) {
            return ((InvocationTargetException) throwable).getCause();
        }
        return throwable;
    }

    private static String required(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new StorageException(path + " cannot be empty.");
        }
        return value.trim();
    }
}
