package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Survives once if killed in a single hit from full health. */
public final class IncompleteCombustionTrait extends Trait {

    public IncompleteCombustionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("incomplete_combustion", "INCOMB", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "ic_used")) {
            return;
        }
        boolean wasFull = mob.getHealth() >= Mobs.maxHealth(mob) - 0.01;
        if (wasFull && mob.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            mob.setHealth(1);
            EntityState.setFlag(mob, "ic_used", Integer.MAX_VALUE);
        }
    }
}
