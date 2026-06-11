package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.PlayerData;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** 各プレイヤーが入った特殊ディメンションを記録する（難易度要素3）。 */
public final class PlayerListener implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        mark(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // ネザー/エンドに直接ログインしたプレイヤーを捕捉する。
        mark(event.getPlayer());
    }

    private void mark(Player player) {
        World.Environment env = player.getWorld().getEnvironment();
        PlayerData data = PlayerData.of(player);
        if (env == World.Environment.NETHER) {
            data.setVisitedNether();
        } else if (env == World.Environment.THE_END) {
            data.setVisitedEnd();
        }
    }
}
