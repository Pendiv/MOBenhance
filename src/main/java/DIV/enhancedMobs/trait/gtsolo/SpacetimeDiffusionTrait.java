package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

/** 時空族: 周囲10m以内のMobに「spacetime」タグを伝播させる。 */
public final class SpacetimeDiffusionTrait extends AuraTrait {

    public SpacetimeDiffusionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_diffusion", "STDIFF", cost, weight, maxRank, minLevel, 10.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        MobTags.add(target, "spacetime");
    }
}
