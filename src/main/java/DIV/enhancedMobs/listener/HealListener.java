package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.HealMultiplier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/** {@link HealMultiplier} 疑似属性を回復量に適用 */
public final class HealListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        double factor = HealMultiplier.effective(entity);
        if (factor == 1.0) {
            return;
        }
        double amount = event.getAmount() * factor;
        event.setAmount(amount);
        if (amount <= 0) {
            event.setCancelled(true);
        }
    }
}
