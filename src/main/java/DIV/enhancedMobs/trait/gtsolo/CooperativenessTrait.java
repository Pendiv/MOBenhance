package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 耐性付与で集団行動時の硬さを表現。 */
public final class CooperativenessTrait extends Trait {

    public CooperativenessTrait(int cost, int weight, int maxRank, int minLevel) {
        super("cooperativeness", "COOP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, 0, true, false, false));
    }
}
