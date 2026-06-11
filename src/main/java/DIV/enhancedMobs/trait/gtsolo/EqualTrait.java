package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;

/** Invulnerable for 5 seconds after first acquiring a target. */
public final class EqualTrait extends Trait {

    public EqualTrait(int cost, int weight, int maxRank, int minLevel) {
        super("equal", "EQUAL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob instanceof Mob asMob && asMob.getTarget() != null && !EntityState.hasFlag(mob, "equal_done")) {
            EntityState.setFlag(mob, "equal_invuln", 100);
            EntityState.setFlag(mob, "equal_done", Integer.MAX_VALUE);
        }
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "equal_invuln")) {
            event.setCancelled(true);
        }
    }
}
