package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Approximation: glowing and empowered (vain show of force). */
public final class VainGloryTrait extends Trait {

    public VainGloryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("vain_glory", "VAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
    }
}
