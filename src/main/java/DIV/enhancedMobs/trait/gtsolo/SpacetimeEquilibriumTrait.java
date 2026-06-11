package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 時空族: 周囲8m以内のプレイヤーおよび非時空族Mobに継続的に弱体化を与える。 */
public final class SpacetimeEquilibriumTrait extends AuraTrait {

    public SpacetimeEquilibriumTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_equilibrium", "STEQUI", cost, weight, maxRank, minLevel, 8.0, TargetKind.ALL);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (target instanceof Player || !MobTags.has(target, "spacetime")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, rank - 1, true, false, false));
        }
    }
}
