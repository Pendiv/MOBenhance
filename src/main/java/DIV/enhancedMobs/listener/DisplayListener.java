package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.gtsolo.SorrowElegyTrait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * ヘッド表示をモブと同期させる。管理対象モブがワールドから消えた際（死亡・デスポーン・/kill・虚空）
 * にタグを除去し、チャンクロード時に再アタッチする。
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
            if (!(entity instanceof LivingEntity mob)) {
                continue;
            }
            // 悲哀の挽歌の仮死監視を復帰（再起動・再ロードで消えた FastTick ウォッチャーの再登録）。
            // 仮死中の被害者は未処理モブのこともあるため isProcessed 判定より前に行う。
            SorrowElegyTrait.rehydrateWatcher(mob);
            if (!MobData.of(mob).isProcessed()) {
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
