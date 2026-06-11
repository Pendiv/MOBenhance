package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** The more health it has lost, the more bonus (armor-piercing) damage it deals. */
public final class DominationOverVictoryTrait extends Trait {

    public DominationOverVictoryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("domination_over_victory", "DOMIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int tiers = (int) ((1.0 - Mobs.healthRatio(mob)) / 0.25);
        event.setDamage(event.getDamage() + tiers * rank);
    }
}
