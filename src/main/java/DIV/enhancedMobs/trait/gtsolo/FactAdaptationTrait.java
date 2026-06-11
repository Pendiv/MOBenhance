package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Strips all absorption (yellow hearts) from whatever it hits. */
public final class FactAdaptationTrait extends Trait {

    public FactAdaptationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("fact_adaptation", "FACT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        target.setAbsorptionAmount(0);
    }
}
