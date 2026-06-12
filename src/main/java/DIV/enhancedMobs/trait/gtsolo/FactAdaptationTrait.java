package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃命中時にターゲットの吸収ハート（黄色ハート）をすべて除去する。飛翔体は射手に解決されるため近接・射撃の両方で発動する。 */
public final class FactAdaptationTrait extends Trait {

    public FactAdaptationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("fact_adaptation", "FACT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        target.setAbsorptionAmount(0);
    }
}
