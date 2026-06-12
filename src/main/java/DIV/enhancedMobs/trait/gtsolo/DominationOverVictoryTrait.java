package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** ボス専用: 自身のHPを25%失うごとに与ダメージが +10%×rank ずつ増幅する。 */
public final class DominationOverVictoryTrait extends Trait {

    public DominationOverVictoryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("domination_over_victory", "DOMIN", cost, weight, maxRank, minLevel);
    }

    /** 原典はボス限定（L2EntityUtil.isBoss）。 */
    @Override
    public boolean appliesTo(LivingEntity mob) {
        return Mobs.isBoss(mob.getType());
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int tiers = (int) ((1.0 - Mobs.healthRatio(mob)) / 0.25);
        if (tiers <= 0) {
            return;
        }
        // 原典: 与ダメ × (1 + tiers × 0.10 × rank)。Bukkit ではこの後の防具計算を固定できない近似
        event.setDamage(event.getDamage() * (1 + tiers * 0.10 * rank));
    }
}
