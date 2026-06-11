package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Generic per-entity scratch state for traits: timed flags (cooldowns, windows) and
 * accumulating numbers (stacks, multipliers). Stored in PDC, works on any entity or player.
 * Reusable building block for porting the GTsolo traits.
 */
public final class EntityState {

    private EntityState() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    /** Set a flag that stays active for {@code durationTicks} (also used as a cooldown). */
    public static void setFlag(PersistentDataHolder holder, String name, int durationTicks) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.LONG,
                (long) Bukkit.getCurrentTick() + durationTicks);
    }

    public static boolean hasFlag(PersistentDataHolder holder, String name) {
        Long until = holder.getPersistentDataContainer().get(key(name), PersistentDataType.LONG);
        return until != null && Bukkit.getCurrentTick() < until;
    }

    public static double getDouble(PersistentDataHolder holder, String name, double def) {
        Double value = holder.getPersistentDataContainer().get(key(name), PersistentDataType.DOUBLE);
        return value == null ? def : value;
    }

    public static void setDouble(PersistentDataHolder holder, String name, double value) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.DOUBLE, value);
    }

    public static double addDouble(PersistentDataHolder holder, String name, double delta) {
        double value = getDouble(holder, name, 0) + delta;
        setDouble(holder, name, value);
        return value;
    }

    public static int getInt(PersistentDataHolder holder, String name, int def) {
        Integer value = holder.getPersistentDataContainer().get(key(name), PersistentDataType.INTEGER);
        return value == null ? def : value;
    }

    public static void setInt(PersistentDataHolder holder, String name, int value) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.INTEGER, value);
    }

    public static int addInt(PersistentDataHolder holder, String name, int delta) {
        int value = getInt(holder, name, 0) + delta;
        setInt(holder, name, value);
        return value;
    }
}
