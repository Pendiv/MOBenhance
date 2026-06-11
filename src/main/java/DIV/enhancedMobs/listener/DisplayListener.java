package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Keeps head displays in sync with their mobs: removes the tag whenever a managed mob leaves the
 * world (death, despawn, /kill, void), and re-attaches tags when chunks load back in.
 */
public final class DisplayListener implements Listener {

    private final EnhancedMobs plugin;

    public DisplayListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getEntity() instanceof LivingEntity mob && MobData.of(mob).isProcessed()) {
            plugin.traitDisplay().cleanup(mob);
        }
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity mob) || !MobData.of(mob).isProcessed()) {
                continue;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (mob.isValid() && !plugin.traitDisplay().hasDisplay(mob)) {
                    int level = MobData.of(mob).getLevel();
                    String text = plugin.traits().displayText(plugin.traits().read(mob));
                    plugin.traitDisplay().attach(mob, level, text);
                }
            });
        }
    }
}
