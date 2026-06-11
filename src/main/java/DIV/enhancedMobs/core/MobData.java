package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * モブの PersistentDataContainer に対する読み書きラッパー。
 *
 * <p>L2Hostility の {@code MobTraitCap} に相当。レベル・特性等の
 * モブ固有の永続状態はすべてここで管理し、core/ 外から PDC を直接触らない。
 */
public final class MobData {

    public static final NamespacedKey LEVEL = key("level");
    public static final NamespacedKey TRAITS = key("traits");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    private final LivingEntity entity;
    private final PersistentDataContainer pdc;

    private MobData(LivingEntity entity) {
        this.entity = entity;
        this.pdc = entity.getPersistentDataContainer();
    }

    public static MobData of(LivingEntity entity) {
        return new MobData(entity);
    }

    /** レベルが付与済みであれば true（二重処理防止）。 */
    public boolean isProcessed() {
        return pdc.has(LEVEL, PersistentDataType.INTEGER);
    }

    public int getLevel() {
        Integer value = pdc.get(LEVEL, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public void setLevel(int level) {
        pdc.set(LEVEL, PersistentDataType.INTEGER, level);
    }

    /** トレイトマップを {@code "tank:3;fiery:1"} 形式で返す。未設定時は空文字列。 */
    public String getTraitsRaw() {
        String value = pdc.get(TRAITS, PersistentDataType.STRING);
        return value == null ? "" : value;
    }

    public void setTraitsRaw(String raw) {
        pdc.set(TRAITS, PersistentDataType.STRING, raw);
    }

    public LivingEntity entity() {
        return entity;
    }
}
