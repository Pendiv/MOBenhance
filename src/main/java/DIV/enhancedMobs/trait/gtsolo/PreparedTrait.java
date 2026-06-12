package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/** スポーン時に最大HP × (20 + 12×rank)% の吸収ハート（生の吸収値、減ったら戻らない）を付与する。 */
public final class PreparedTrait extends Trait {

    public PreparedTrait(int cost, int weight, int maxRank, int minLevel) {
        super("prepared", "PREP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        apply(mob, rank);
        // TANK 等が最大HPを増やす順序は不定のため、確定後の最大HPで1tick後に再計算する
        Bukkit.getScheduler().runTask(EnhancedMobs.get(), () -> {
            if (mob.isValid()) {
                apply(mob, rank);
            }
        });
    }

    private static void apply(LivingEntity mob, int rank) {
        mob.setAbsorptionAmount(Mobs.maxHealth(mob) * (0.20 + 0.12 * rank));
    }
}
