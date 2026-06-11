package DIV.enhancedMobs.task;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * tick ベーストレイト（SHULKER・KILLER_AURA・PULLING・REPELLING 等）の定期実行タスク。
 *
 * <p>全ワールドの生物エンティティをスキャンし、当プラグインの PDC を持つものだけ tick する。
 * エンティティ数が増大した場合は管理 UUID セットへの切り替えを検討。
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
