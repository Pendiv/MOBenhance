package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

/** Creeper with a much shorter fuse. */
public final class HairTriggerTrait extends Trait {

    public HairTriggerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("hair_trigger", "HAIR", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (mob instanceof Creeper creeper) {
            creeper.setMaxFuseTicks(Math.max(5, 30 - 4 * rank));
        }
    }
}
