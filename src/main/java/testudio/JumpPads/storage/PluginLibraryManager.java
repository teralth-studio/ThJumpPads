package testudio.JumpPads.storage;

import testudio.JumpPads.JumpPadsPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginLibraryManager implements AutoCloseable {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 30000;
    private static final long MAX_LIBRARY_SIZE = 100L * 1024L * 1024L;

    private static final Library SQLITE = new Library(
            "org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar",
            "sqlite-jdbc-3.50.3.0.jar",
            "a3f53a2aa15ae9425a9e793bbe9c8e5288febeb4b65ef5c1a4e80d4c2045cf08");
    private static final Library MYSQL = new Library(
            "com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar",
            "mysql-connector-j-9.4.0.jar",
            "49ed93c8b2bea9cb0929b85a8a28837b191d0f8eac6919fdcef16e36e2cd53b3");
    private static final Library PROTOBUF = new Library(
            "com/google/protobuf/protobuf-java/4.31.1/protobuf-java-4.31.1.jar",
            "protobuf-java-4.31.1.jar",
            "d60dfe7c68a0d38a248cca96924f289dc7e1966a887ee7cae397701af08575ae");
    private static final Library MONGO_SYNC = new Library(
            "org/mongodb/mongodb-driver-sync/5.5.1/mongodb-driver-sync-5.5.1.jar",
            "mongodb-driver-sync-5.5.1.jar",
            "10fbba300aca76d6996ea8a842226e78bcaaf108c908687940c991885be5e875");
    private static final Library MONGO_CORE = new Library(
            "org/mongodb/mongodb-driver-core/5.5.1/mongodb-driver-core-5.5.1.jar",
            "mongodb-driver-core-5.5.1.jar",
            "9bd21650f6c0688461a5de30000efb67ec1d5d81a1e80c5c4928bb50bbca3f16");
    private static final Library BSON = new Library(
            "org/mongodb/bson/5.5.1/bson-5.5.1.jar",
            "bson-5.5.1.jar",
            "99fd39f8b313ca5f39cb1a2e62454283a592dc4227c9010909e3652ef578a274");
    private static final Library BSON_RECORD = new Library(
            "org/mongodb/bson-record-codec/5.5.1/bson-record-codec-5.5.1.jar",
            "bson-record-codec-5.5.1.jar",
            "558fbca5b564b9335809eccbe46168abd41b04350599e43e1713cebccaa010db");

    private final File libraryDirectory;
    private final ClassLoader parentClassLoader;
    private final Map<String, URLClassLoader> classLoaders = new LinkedHashMap<String, URLClassLoader>();

    public PluginLibraryManager(JumpPadsPlugin plugin) {
        this(new File(plugin.getDataFolder(), "lib"), plugin.getClass().getClassLoader());
    }

    PluginLibraryManager(File libraryDirectory, ClassLoader parentClassLoader) {
        this.libraryDirectory = libraryDirectory;
        this.parentClassLoader = parentClassLoader;
    }

    public synchronized ClassLoader classLoaderFor(String backend) {
        URLClassLoader existing = classLoaders.get(backend);
        if (existing != null) {
            return existing;
        }

        List<Library> libraries = librariesFor(backend);
        List<URL> urls = new ArrayList<URL>(libraries.size());
        for (Library library : libraries) {
            urls.add(ensureLibrary(library));
        }
        URLClassLoader loader = new IsolatedLibraryClassLoader(urls.toArray(new URL[0]), parentClassLoader);
        classLoaders.put(backend, loader);
        return loader;
    }

    @Override
    public synchronized void close() {
        IOException failure = null;
        for (URLClassLoader loader : classLoaders.values()) {
            try {
                loader.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        classLoaders.clear();
        if (failure != null) {
            throw new StorageException("Could not close the storage library class loaders.", failure);
        }
    }

    private URL ensureLibrary(Library library) {
        try {
            Files.createDirectories(libraryDirectory.toPath());
            Path target = libraryDirectory.toPath().resolve(library.fileName);
            if (Files.isRegularFile(target) && library.sha256.equals(sha256(target))) {
                return target.toUri().toURL();
            }

            Path temporary = Files.createTempFile(libraryDirectory.toPath(), library.fileName, ".part");
            try {
                download(library, temporary);
                if (!library.sha256.equals(sha256(temporary))) {
                    throw new IOException("SHA-256 verification failed for " + library.fileName + ".");
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return target.toUri().toURL();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new StorageException("Could not download or verify " + library.fileName
                    + " from Maven Central.", e);
        }
    }

    private static void download(Library library, Path target) throws IOException {
        URL url = new URL(MAVEN_CENTRAL + library.mavenPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "ThJumpPads/1.0");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Maven Central returned HTTP " + status + " for " + library.fileName + ".");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_LIBRARY_SIZE) {
                throw new IOException("Library exceeds the permitted download size: " + library.fileName + ".");
            }

            long totalBytes = 0L;
            byte[] buffer = new byte[8192];
            try (InputStream input = connection.getInputStream();
                 OutputStream output = Files.newOutputStream(target)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    totalBytes += count;
                    if (totalBytes > MAX_LIBRARY_SIZE) {
                        throw new IOException("Library exceeds the permitted download size: "
                                + library.fileName + ".");
                    }
                    output.write(buffer, 0, count);
                }
            }
            if (contentLength >= 0L && totalBytes != contentLength) {
                throw new IOException("Incomplete download for " + library.fileName + ".");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
            while (input.read(buffer) != -1) {
                // Consume the entire file to calculate its digest.
            }
        }
        byte[] hash = digest.digest();
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static List<Library> librariesFor(String backend) {
        if ("sqlite".equals(backend)) {
            return Arrays.asList(SQLITE);
        }
        if ("mysql".equals(backend)) {
            return Arrays.asList(MYSQL, PROTOBUF);
        }
        if ("mongodb".equals(backend)) {
            return Arrays.asList(MONGO_SYNC, MONGO_CORE, BSON, BSON_RECORD);
        }
        throw new StorageException("Unsupported storage backend: " + backend);
    }

    private static final class Library {

        private final String mavenPath;
        private final String fileName;
        private final String sha256;

        private Library(String mavenPath, String fileName, String sha256) {
            this.mavenPath = mavenPath;
            this.fileName = fileName;
            this.sha256 = sha256;
        }
    }

    private static final class IsolatedLibraryClassLoader extends URLClassLoader {

        private IsolatedLibraryClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!isLibraryClass(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try {
                        loaded = findClass(name);
                    } catch (ClassNotFoundException e) {
                        loaded = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private static boolean isLibraryClass(String name) {
            return name.startsWith("org.sqlite.")
                    || name.startsWith("com.mysql.")
                    || name.startsWith("com.google.protobuf.")
                    || name.startsWith("com.mongodb.")
                    || name.startsWith("org.bson.");
        }
    }
}
