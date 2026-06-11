package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Per-player, per-trait cooldown that escalates under sustained pressure. For "undefendable" traits
 * (auras, position-swaps, knockback volleys), this stops a single player from being chain-locked:
 * once affected, they get a cooldown, and being hit again before it resets makes the next cooldown
 * longer (up to a cap). A long enough break resets the escalation.
 *
 * <p>Non-players are never put on cooldown (returns true) — only players get the protection.
 */
public final class TraitCooldown {

    private TraitCooldown() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    /** Convenience: base 2s, +1s per consecutive hit, cap 5 stacks, reset after 10s. */
    public static boolean ready(LivingEntity target, String traitId) {
        return ready(target, traitId, 40, 20, 5, 200);
    }

    /**
     * @return true if the trait may affect this target now. On true for a player, registers the hit
     *         and (re)sets the escalating cooldown.
     */
    public static boolean ready(LivingEntity target, String traitId, int baseTicks, int stepTicks,
                                int maxStacks, int resetWindowTicks) {
        if (!(target instanceof Player)) {
            return true;
        }
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        NamespacedKey untilKey = key("ct_" + traitId + "_until");
        NamespacedKey lastKey = key("ct_" + traitId + "_last");
        NamespacedKey stackKey = key("ct_" + traitId + "_stk");

        long now = Bukkit.getCurrentTick();
        Long until = pdc.get(untilKey, PersistentDataType.LONG);
        if (until != null && now < until) {
            return false;
        }

        long last = pdc.getOrDefault(lastKey, PersistentDataType.LONG, -1_000_000L);
        int stacks = pdc.getOrDefault(stackKey, PersistentDataType.INTEGER, 0);
        if (now - last > resetWindowTicks) {
            stacks = 0;
        }
        int cooldown = baseTicks + Math.min(stacks, maxStacks) * stepTicks;
        pdc.set(untilKey, PersistentDataType.LONG, now + cooldown);
        pdc.set(lastKey, PersistentDataType.LONG, now);
        pdc.set(stackKey, PersistentDataType.INTEGER, stacks + 1);
        return true;
    }
}
