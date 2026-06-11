package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/** On death, drags the world into night. */
public final class SpacetimeConformityTrait extends Trait {

    public SpacetimeConformityTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_conformity", "STCONF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        mob.getWorld().setTime(13000);
    }
}
