package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 近似実装 ADAPTIVE: 同じ原因の攻撃が連続するほど被ダメージを減少させる。
 * バニラの {@code DamageCause} はL2のダメージ種別より粗いが、同一原因の連打で耐性が積み上がる。
 */
public final class AdaptiveTrait extends Trait {

    private final double reductionPerStack;
    private final double maxReduction;
    private final NamespacedKey causeKey;
    private final NamespacedKey countKey;

    public AdaptiveTrait(int cost, int weight, int maxRank, int minLevel,
                         double reductionPerStack, double maxReduction) {
        super("adaptive", "ADAPT", cost, weight, maxRank, minLevel);
        this.reductionPerStack = reductionPerStack;
        this.maxReduction = maxReduction;
        this.causeKey = new NamespacedKey(EnhancedMobs.get(), "adapt_cause");
        this.countKey = new NamespacedKey(EnhancedMobs.get(), "adapt_count");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        String cause = event.getCause().name();
        String previous = pdc.get(causeKey, PersistentDataType.STRING);
        int count = cause.equals(previous) ? pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1 : 1;
        pdc.set(causeKey, PersistentDataType.STRING, cause);
        pdc.set(countKey, PersistentDataType.INTEGER, count);

        double reduction = Math.min(maxReduction, reductionPerStack * rank * (count - 1));
        if (reduction > 0) {
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }
}
