package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** A killing blow from this mob denies the victim's totem revive. */
public final class SoulDestructionTrait extends Trait {

    public SoulDestructionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("soul_destruction", "SOULDST", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target.getHealth() - event.getFinalDamage() <= 0) {
            EntityState.setFlag(target, "deny_resurrect", 40);
        }
    }
}
