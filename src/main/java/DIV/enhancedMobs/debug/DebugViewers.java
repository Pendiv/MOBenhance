package DIV.enhancedMobs.debug;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * デバッグ表示が有効なプレイヤーのインメモリセット。
 *
 * <p>{@code debug/} パッケージ全体が開発用足場。リリース前に削除または縮小すること。
 */
public final class DebugViewers {

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    /** @return ON になった場合 true、OFF になった場合 false。 */
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
