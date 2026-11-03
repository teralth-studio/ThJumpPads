package testudio.JumpPads.storage;

import java.util.UUID;

public final class StoredToggle {

    private final UUID uuid;
    private final String nameKey;
    private final boolean enabled;

    public StoredToggle(UUID uuid, String nameKey, boolean enabled) {
        this.uuid = uuid;
        this.nameKey = nameKey;
        this.enabled = enabled;
    }

    public UUID uuid() {
        return uuid;
    }

    public String nameKey() {
        return nameKey;
    }

    public boolean enabled() {
        return enabled;
    }
}
