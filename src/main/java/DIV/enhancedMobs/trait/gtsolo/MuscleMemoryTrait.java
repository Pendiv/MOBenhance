package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 永続的な力効果（筋肉記憶による攻撃力強化）。 */
public final class MuscleMemoryTrait extends Trait {

    public MuscleMemoryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("muscle_memory", "MUSCLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
    }
}
