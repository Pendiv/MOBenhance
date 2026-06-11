package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 時空族: 近似実装として永続力増強（ランクに応じたレベル）を付与する。 */
public final class SpacetimeHeroTrait extends Trait {

    public SpacetimeHeroTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_hero", "STHERO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
    }
}
