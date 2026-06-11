package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Read/write wrapper over a mob's PersistentDataContainer.
 *
 * <p>This is the plugin equivalent of L2Hostility's {@code MobTraitCap}: all per-mob
 * persistent state (level, traits, init stage) lives here, and nothing outside core/
 * touches the raw PDC.
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

    /** True once this mob has been assigned a level (so we never process it twice). */
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

    /** Trait map serialized as {@code "tank:3;fiery:1"}. Empty string when none. */
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
