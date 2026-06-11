package DIV.enhancedMobs.level;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.config.MainConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies level-based stat enhancement, distributed for variety ("振れ").
 *
 * <p>Four stats — health, armor, attack, speed — each gain {@code count * perStep}. The counts
 * come from the level N:
 * <ul>
 *   <li>fine part {@code ones = N % 10}: that many +1 draws, each to a random stat;</li>
 *   <li>coarse part {@code high = N / 10}: exactly {@code coarseDraws} (≈10) draws of {@code +high},
 *       each to a random stat.</li>
 * </ul>
 * Total applications = {@code 10*high + ones = N}, but only ~19 draws regardless of level, so the
 * per-stat spread is intentionally coarse — some mobs end up specced hard into one stat.
 */
public final class LevelScaler {

    private static final int HEALTH = 0;
    private static final int ARMOR = 1;
    private static final int ATTACK = 2;
    private static final int SPEED = 3;

    private final MainConfig config;
    private final NamespacedKey healthKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey attackKey;
    private final NamespacedKey speedKey;

    public LevelScaler(EnhancedMobs plugin, MainConfig config) {
        this.config = config;
        this.healthKey = new NamespacedKey(plugin, "enh_health");
        this.armorKey = new NamespacedKey(plugin, "enh_armor");
        this.attackKey = new NamespacedKey(plugin, "enh_attack");
        this.speedKey = new NamespacedKey(plugin, "enh_speed");
    }

    public void apply(LivingEntity mob, int level) {
        int[] counts = allocate(level);

        applyFlat(mob, Attribute.MAX_HEALTH, healthKey, counts[HEALTH] * config.enhHealth);
        applyFlat(mob, Attribute.ARMOR, armorKey, counts[ARMOR] * config.enhArmor);
        applyFlat(mob, Attribute.ATTACK_DAMAGE, attackKey, counts[ATTACK] * config.enhAttack);

        double speedBonus = Math.min(counts[SPEED] * config.enhSpeed, config.maxSpeedBonus);
        applyFlat(mob, Attribute.MOVEMENT_SPEED, speedKey, speedBonus);

        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            mob.setHealth(maxHealth.getValue());
        }
    }

    private int[] allocate(int level) {
        int[] counts = new int[4];
        int ones = level % 10;
        int high = level / 10;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (high > 0) {
            for (int i = 0; i < config.coarseDraws; i++) {
                counts[random.nextInt(4)] += high;
            }
        }
        for (int i = 0; i < ones; i++) {
            counts[random.nextInt(4)] += 1;
        }
        return counts;
    }

    private void applyFlat(LivingEntity mob, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return; // e.g. ATTACK_DAMAGE is absent on creepers / ranged-only mobs
        }
        inst.getModifiers().stream()
                .filter(m -> key.equals(m.getKey()))
                .toList()
                .forEach(inst::removeModifier);
        if (amount != 0) {
            inst.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
