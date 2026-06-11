package DIV.enhancedMobs.debug;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory set of players who have the debug readout enabled.
 *
 * <p>This whole {@code debug/} package is development scaffolding — trim or delete it before the
 * final build.
 */
public final class DebugViewers {

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    /** @return true if now ON, false if now OFF. */
    public boolean toggle(UUID id) {
        if (!viewers.add(id)) {
            viewers.remove(id);
            return false;
        }
        return true;
    }

    public boolean isViewing(UUID id) {
        return viewers.contains(id);
    }
}
