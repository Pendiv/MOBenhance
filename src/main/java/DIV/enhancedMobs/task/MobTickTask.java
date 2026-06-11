package DIV.enhancedMobs.task;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * Periodic ticker for tick-based traits (SHULKER, KILLER_AURA, PULLING, REPELLING).
 *
 * <p>Scans living entities in each world; only those carrying our PDC are ticked. Fine for now;
 * if entity counts grow, switch to a tracked Set of managed UUIDs.
 */
public final class MobTickTask implements Runnable {

    private final EnhancedMobs plugin;

    public MobTickTask(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (MobData.of(entity).isProcessed()) {
                    plugin.traits().tick(entity);
                }
            }
        }
    }
}
