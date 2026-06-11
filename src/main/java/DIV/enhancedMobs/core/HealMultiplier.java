package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * A plugin-side "healing_received" pseudo-attribute, modelled on AttributesLib's attribute of the
 * same name (base 1.0, 0 = no healing). Real attributes can't be registered at runtime, so the
 * value lives in PDC and is applied by {@code HealListener} hooking EntityRegainHealthEvent.
 *
 * <p>Supports a permanent base factor plus a timed "curse" override (used by CURSED).
 */
public final class HealMultiplier {

    private static final NamespacedKey BASE = key("heal_base");
    private static final NamespacedKey CURSE_MULT = key("heal_curse_mult");
    private static final NamespacedKey CURSE_UNTIL = key("heal_curse_until");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    private HealMultiplier() {
    }

    /** Effective heal factor for this entity right now (1.0 = unchanged). */
    public static double effective(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        double factor = pdc.getOrDefault(BASE, PersistentDataType.DOUBLE, 1.0);
        Long until = pdc.get(CURSE_UNTIL, PersistentDataType.LONG);
        if (until != null && Bukkit.getCurrentTick() < until) {
            factor *= pdc.getOrDefault(CURSE_MULT, PersistentDataType.DOUBLE, 1.0);
        }
        return factor;
    }

    /** Temporarily multiply the entity's healing (CURSED). mult 0 = no healing for the duration. */
    public static void applyCurse(LivingEntity entity, double mult, int durationTicks) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(CURSE_MULT, PersistentDataType.DOUBLE, mult);
        pdc.set(CURSE_UNTIL, PersistentDataType.LONG, (long) Bukkit.getCurrentTick() + durationTicks);
    }
}
