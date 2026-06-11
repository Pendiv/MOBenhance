package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 6ブロック以内にプレイヤーが近づくまで透明状態を維持する。 */
public final class SpacetimeShadowTrait extends Trait {

    public SpacetimeShadowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_shadow", "STSHADOW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1_000_000, 0, true, false, false));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (Mobs.nearestPlayer(mob, 6) != null) {
            mob.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false, false));
        }
    }
}
