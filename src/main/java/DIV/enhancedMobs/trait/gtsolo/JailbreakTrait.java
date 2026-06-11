package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 力+速度で強化する。 */
public final class JailbreakTrait extends Trait {

    public JailbreakTrait(int cost, int weight, int maxRank, int minLevel) {
        super("jailbreak", "JAIL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1_000_000, 0, true, false, false));
    }
}
