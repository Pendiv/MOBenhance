package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** スポーン時に永続的な吸収ハートを付与する。 */
public final class PreparedTrait extends Trait {

    public PreparedTrait(int cost, int weight, int maxRank, int minLevel) {
        super("prepared", "PREP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1_000_000, rank - 1, true, false, false));
    }
}
