package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.trait.gtsolo.SorrowElegyTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeShadowRaidTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * 回復イベントの特性フック。
 * 回復倍率の適用は attributelib（heal_multiplier 標準属性）が HIGH で行うため、ここでは扱わない。
 */
public final class HealListener implements Listener {

    /** 悲哀の挽歌: 仮死中のエンティティは回復不可。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPseudoDeathHeal(EntityRegainHealthEvent event) {
        SorrowElegyTrait.preventHeal(event);
    }

    /** 隠蔽（時空の影襲）の回復返済。確定した回復量で判定するため MONITOR で拾う。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConcealRepay(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            SpacetimeShadowRaidTrait.onHeal(entity, event);
        }
    }
}
