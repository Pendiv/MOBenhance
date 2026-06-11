package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** Marker: tags the mob as part of the "spacetime" family. */
public final class SpacetimeEntryTrait extends Trait {

    public SpacetimeEntryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_entry", "STENTRY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }
}
