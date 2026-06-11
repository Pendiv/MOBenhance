package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.EntityState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

/**
 * Lets "certain-kill" traits deny totem-of-undying revives: a trait sets the {@code deny_resurrect}
 * timed flag on its victim, and this cancels the resurrect while the flag is active.
 */
public final class ResurrectListener implements Listener {

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (EntityState.hasFlag(event.getEntity(), "deny_resurrect")) {
            event.setCancelled(true);
        }
    }
}
