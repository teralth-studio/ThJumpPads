package testudio.JumpPads.storage;

import java.util.List;
import java.util.UUID;

public interface StorageBackend extends AutoCloseable {

    List<StoredToggle> load();

    void save(UUID uuid, String nameKey, boolean enabled);

    @Override
    void close();
}
