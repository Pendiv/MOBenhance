package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

/** 時空族: 周囲12m以内の時空族Mobを毎ティックランク分だけ回復させる。 */
public final class SpacetimeDevotionTrait extends AuraTrait {

    public SpacetimeDevotionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_devotion", "STDEVO", cost, weight, maxRank, minLevel, 12.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (MobTags.has(target, "spacetime")) {
            target.setHealth(Math.min(Mobs.maxHealth(target), target.getHealth() + rank));
        }
    }
}
