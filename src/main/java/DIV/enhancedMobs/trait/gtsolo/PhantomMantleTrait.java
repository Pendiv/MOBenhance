package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Approximation: a protective mantle (strong resistance). */
public final class PhantomMantleTrait extends Trait {

    public PhantomMantleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantom_mantle", "MANTLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, Math.min(3, rank), true, false, false));
    }
}
