package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;

/** 死亡時に周囲のMobを回復する。 */
public final class DevotionTrait extends Trait {

    public DevotionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("devotion", "DEVOTION", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        double heal = Mobs.maxHealth(mob) * (0.25 + 0.125 * rank);
        for (Entity entity : mob.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Mob other) {
                other.setHealth(Math.min(Mobs.maxHealth(other), other.getHealth() + heal));
            }
        }
    }
}
