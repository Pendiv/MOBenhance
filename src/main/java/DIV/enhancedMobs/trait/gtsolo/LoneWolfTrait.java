package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Hits harder when no other mobs are nearby. */
public final class LoneWolfTrait extends Trait {

    public LoneWolfTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lone_wolf", "LONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        for (Entity entity : mob.getNearbyEntities(16, 16, 16)) {
            if (entity instanceof Mob) {
                return;
            }
        }
        event.setDamage(event.getDamage() * (1.0 + 0.25 * rank));
    }
}
