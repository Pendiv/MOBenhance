package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * プレイヤー固有の永続状態（PDC）。L2Hostility の {@code PlayerDifficulty} に相当。
 * 現状はネザー・エンド訪問フラグと難易度オフセットを保持。
 */
public final class PlayerData {

    public static final NamespacedKey VISITED_NETHER = key("visited_nether");
    public static final NamespacedKey VISITED_END = key("visited_end");
    public static final NamespacedKey DIFFICULTY_OFFSET = key("difficulty_offset");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    private final PersistentDataContainer pdc;

    private PlayerData(Player player) {
        this.pdc = player.getPersistentDataContainer();
    }

    public static PlayerData of(Player player) {
        return new PlayerData(player);
    }

    public boolean hasVisitedNether() {
        return pdc.has(VISITED_NETHER, PersistentDataType.BYTE);
    }

    public boolean hasVisitedEnd() {
        return pdc.has(VISITED_END, PersistentDataType.BYTE);
    }

    public void setVisitedNether() {
        pdc.set(VISITED_NETHER, PersistentDataType.BYTE, (byte) 1);
    }

    public void setVisitedEnd() {
        pdc.set(VISITED_END, PersistentDataType.BYTE, (byte) 1);
    }

    /** OP が設定する難易度手動調整値。算出値に加算される。 */
    public double getDifficultyOffset() {
        Double value = pdc.get(DIFFICULTY_OFFSET, PersistentDataType.DOUBLE);
        return value == null ? 0.0 : value;
    }

    public void setDifficultyOffset(double value) {
        pdc.set(DIFFICULTY_OFFSET, PersistentDataType.DOUBLE, value);
    }
}
