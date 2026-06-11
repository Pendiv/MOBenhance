package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 近似実装 DEMENTOR: 物理攻撃への耐性と追加ダメージを持つ（ピアスの近似）。 */
public final class DementorTrait extends Trait {

    private final double resistPerRank;
    private final double bonusPerRank;

    public DementorTrait(int cost, int weight, int maxRank, int minLevel,
                         double resistPerRank, double bonusPerRank) {
        super("dementor", "DEMEN", cost, weight, maxRank, minLevel);
        this.resistPerRank = resistPerRank;
        this.bonusPerRank = bonusPerRank;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> {
                double resist = Math.min(0.8, resistPerRank * rank);
                event.setDamage(event.getDamage() * (1.0 - resist));
            }
            default -> {
            }
        }
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() + bonusPerRank * rank);
    }
}
