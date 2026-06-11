package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: クリスタルシールドの代替として高レベルの耐性+再生を付与する。 */
public final class DragonicHeartTrait extends Trait {

    public DragonicHeartTrait(int cost, int weight, int maxRank, int minLevel) {
        super("dragonic_heart", "DRAGON", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, Math.min(4, rank + 1), true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1_000_000, 0, true, false, false));
    }
}
