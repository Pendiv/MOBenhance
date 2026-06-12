package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーのHP割合が高いほど与ダメージを増幅する（rank1満タンで×1.48、rank5で×1.80）。 */
public final class WellFedStrikeTrait extends Trait {

    public WellFedStrikeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("well_fed_strike", "WFSTRK", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player)) {
            return;
        }
        // 原典: M = HP割合(0〜100)、倍率 = 1 + (M + 20×rank) × 0.4%（常に増幅）。
        double m = Math.max(0.0, Math.min(1.0, Mobs.healthRatio(target))) * 100.0;
        event.setDamage(event.getDamage() * (1.0 + (m + 20.0 * rank) * 0.004));
    }
}
