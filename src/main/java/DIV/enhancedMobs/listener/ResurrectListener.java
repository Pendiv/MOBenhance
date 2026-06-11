package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.EntityState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

/**
 * 「確殺」系特性が不死のトーテム復活を阻止できるようにする。
 * トレイトが対象に {@code deny_resurrect} タイムドフラグをセットし、
 * フラグが有効な間は蘇生をキャンセルする。
 * トーテムのみの封殺であるため、その他の復活効果がもしあれば素通りする
 */
public final class ResurrectListener implements Listener {

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (EntityState.hasFlag(event.getEntity(), "deny_resurrect")) {
            event.setCancelled(true);
        }
    }
}
