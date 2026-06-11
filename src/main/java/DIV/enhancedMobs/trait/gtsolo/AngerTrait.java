package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Damage scales up as the mob loses health. */
public final class AngerTrait extends Trait {

    public AngerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("anger", "ANGER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        double missing = 1.0 - Mobs.healthRatio(mob);
        event.setDamage(event.getDamage() * (1.0 + missing * 0.2 * rank));
    }
}
