package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Fully heals when it has not been hit for a while. */
public final class SpacetimeGapTrait extends Trait {

    public SpacetimeGapTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_gap", "STGAP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityState.setFlag(mob, "gap_hit", Math.max(40, 200 - 40 * rank));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!EntityState.hasFlag(mob, "gap_hit")) {
            mob.setHealth(Mobs.maxHealth(mob));
        }
    }
}
