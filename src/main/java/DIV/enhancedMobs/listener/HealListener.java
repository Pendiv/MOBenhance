package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.trait.gtsolo.SorrowElegyTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeShadowRaidTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/** {@link HealMultiplier} 疑似属性を回復量に適用 */
public final class HealListener implements Listener {

    /** 悲哀の挽歌: 仮死中のエンティティは回復不可。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPseudoDeathHeal(EntityRegainHealthEvent event) {
        SorrowElegyTrait.preventHeal(event);
    }

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

    /** 隠蔽（時空の影襲）の回復返済。確定した回復量で判定するため MONITOR で拾う。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConcealRepay(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            SpacetimeShadowRaidTrait.onHeal(entity, event);
        }
    }
}
