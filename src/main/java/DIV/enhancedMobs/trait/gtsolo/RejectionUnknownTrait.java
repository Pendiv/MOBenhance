package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** 同じダメージ種別を受けるたびに耐性が累積し、最大80%軽減する。 */
public final class RejectionUnknownTrait extends Trait {

    private final NamespacedKey causeKey;
    private final NamespacedKey countKey;

    public RejectionUnknownTrait(int cost, int weight, int maxRank, int minLevel) {
        super("rejection_unknown", "REJECT", cost, weight, maxRank, minLevel);
        this.causeKey = new NamespacedKey(EnhancedMobs.get(), "reject_cause");
        this.countKey = new NamespacedKey(EnhancedMobs.get(), "reject_count");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        String cause = event.getCause().name();
        String previous = pdc.get(causeKey, PersistentDataType.STRING);
        int count = cause.equals(previous) ? pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1 : 1;
        pdc.set(causeKey, PersistentDataType.STRING, cause);
        pdc.set(countKey, PersistentDataType.INTEGER, count);
        double reduction = Math.min(0.8, 0.1 * rank * (count - 1));
        if (reduction > 0) {
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }
}
