package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃命中時、ランク×80ティック炎上させる。 */
public final class SkyScorchingFlameTrait extends Trait {

    public SkyScorchingFlameTrait(int cost, int weight, int maxRank, int minLevel) {
        super("sky_scorching_flame", "SCORCH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        target.setFireTicks(Math.max(target.getFireTicks(), 80 * rank));
    }
}
