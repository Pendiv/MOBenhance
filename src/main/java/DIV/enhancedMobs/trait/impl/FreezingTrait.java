package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** ヒット時に対象のフリーズティックを加算する（粉雪のフリーズ演出＋ダメージを流用）。 */
public final class FreezingTrait extends Trait {

    private final int ticksPerRank;

    public FreezingTrait(int cost, int weight, int maxRank, int minLevel, int ticksPerRank) {
        super("freezing", "FREEZE", cost, weight, maxRank, minLevel);
        this.ticksPerRank = ticksPerRank;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int added = target.getFreezeTicks() + ticksPerRank * rank;
        target.setFreezeTicks(Math.min(target.getMaxFreezeTicks() + ticksPerRank * rank, added));
    }
}
