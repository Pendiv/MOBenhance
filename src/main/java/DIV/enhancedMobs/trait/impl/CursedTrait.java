package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** ヒット後一定時間、対象の回復量を削減する（高ランクでほぼ回復不能）。 */
public final class CursedTrait extends Trait {

    private final double reductionPerRank;
    private final int durationTicks;

    public CursedTrait(int cost, int weight, int maxRank, int minLevel,
                       double reductionPerRank, int durationTicks) {
        super("cursed", "CURSE", cost, weight, maxRank, minLevel);
        this.reductionPerRank = reductionPerRank;
        this.durationTicks = durationTicks;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }
        double mult = Math.max(0.0, 1.0 - reductionPerRank * rank);
        HealMultiplier.applyCurse(target, mult, durationTicks);
    }
}
