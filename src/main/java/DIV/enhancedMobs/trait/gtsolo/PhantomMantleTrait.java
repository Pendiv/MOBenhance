package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 強力な耐性効果で全ダメージを軽減する防護マント。 */
public final class PhantomMantleTrait extends Trait {

    public PhantomMantleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantom_mantle", "MANTLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, Math.min(3, rank), true, false, false));
    }
}
