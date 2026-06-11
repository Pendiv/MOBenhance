package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 永続の筋力バフで勢いを攻撃力に変換する。 */
public final class TrujilloHardinTrait extends Trait {

    public TrujilloHardinTrait(int cost, int weight, int maxRank, int minLevel) {
        super("trujillo_hardin", "TRUJILLO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
    }
}
