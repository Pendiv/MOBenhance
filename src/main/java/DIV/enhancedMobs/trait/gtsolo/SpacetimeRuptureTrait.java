package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 対象のHP欠損率に応じてダメージを増幅する（欠損100%でrank×50%増）。 */
public final class SpacetimeRuptureTrait extends Trait {

    public SpacetimeRuptureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_rupture", "STRUPT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        double missing = 1.0 - Mobs.healthRatio(target);
        event.setDamage(event.getDamage() * (1.0 + missing * 0.5 * rank));
    }
}
