package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Blocks physical damage; only magic gets through. */
public final class SpacetimeDiveTrait extends Trait {

    public SpacetimeDiveTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_dive", "STDIVE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> event.setCancelled(true);
            default -> {
            }
        }
    }
}
