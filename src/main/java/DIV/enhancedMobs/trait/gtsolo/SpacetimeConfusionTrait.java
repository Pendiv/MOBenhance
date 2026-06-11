package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 時空族: 周囲18m以内のMobに継続的に弱体化を与える。 */
public final class SpacetimeConfusionTrait extends AuraTrait {

    public SpacetimeConfusionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_confusion", "STCONFU", cost, weight, maxRank, minLevel, 18.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, rank - 1, true, false, false));
    }
}
