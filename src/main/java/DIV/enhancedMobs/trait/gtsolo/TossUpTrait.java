package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Launches the target upward on hit. */
public final class TossUpTrait extends Trait {

    public TossUpTrait(int cost, int weight, int maxRank, int minLevel) {
        super("toss_up", "TOSS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        target.setVelocity(target.getVelocity().setY(0.8 + 0.1 * rank));
    }
}
