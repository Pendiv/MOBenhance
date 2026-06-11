package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近傍プレイヤーに弱体効果を付与するオーラ。 */
public final class SpacetimeShadowRaidTrait extends AuraTrait {

    public SpacetimeShadowRaidTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_shadow_raid", "STRAID", cost, weight, maxRank, minLevel, 12.0, TargetKind.PLAYERS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, rank - 1, true, true, true));
    }
}
