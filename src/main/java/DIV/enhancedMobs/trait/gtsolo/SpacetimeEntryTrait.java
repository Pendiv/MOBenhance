package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** Mobに「spacetime」タグを付与するだけのマーカートレイト。 */
public final class SpacetimeEntryTrait extends Trait {

    public SpacetimeEntryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_entry", "STENTRY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }
}
