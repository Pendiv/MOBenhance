package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 自身に火耐性を付与し、ヒット時に対象を着火する。 */
public final class FieryTrait extends Trait {

    private static final int LONG_DURATION = 1_000_000;

    private final int fireSeconds;

    public FieryTrait(int cost, int weight, int maxRank, int minLevel, int fireSeconds) {
        super("fiery", "FIERY", cost, weight, maxRank, minLevel);
        this.fireSeconds = fireSeconds;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, LONG_DURATION, 0, true, false, false));
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }
        target.setFireTicks(Math.max(target.getFireTicks(), fireSeconds * 20));
    }
}
