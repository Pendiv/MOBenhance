package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.world.AmeijiaGate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.entity.Player;

/**
 * ゲート（END_GATEWAY、互換のため END_PORTAL も）によるアメイジアの出入りを処理する。
 * <ul>
 *   <li>アメイジア内のゲート → 元の場所へ帰還（バニラのエンド送りを打ち消す）。</li>
 *   <li>登録済みゲート → アメイジアへ。</li>
 *   <li>それ以外（バニラのエンドゲートウェイ/ポータル）→ 何もせずバニラ任せ。</li>
 * </ul>
 */
public final class GateListener implements Listener {

    /** END_GATEWAY 経由（基幹ゲート・コマンドゲートの本線）。 */
    @EventHandler(ignoreCancelled = false)
    public void onGateway(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.END_GATEWAY) {
            handle(event, event.getPlayer());
        }
    }

    /** END_PORTAL 経由（互換用。バニラのエンドポータルは未登録なので素通り）。 */
    @EventHandler(ignoreCancelled = false)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.END_PORTAL) {
            handle(event, event.getPlayer());
        }
    }

    /**
     * フォールバック: END_GATEWAY のネイティブTPが発火しない場合に備え、
     * ゲートウェイの中へ入った（ブロックが変わった瞬間）を直接検知してテレポートする。
     * 多重発火は {@link AmeijiaGate} 側の処理中ガードで吸収する。
     */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || !movedBlock(event.getFrom(), to)) {
            return;
        }
        if (!AmeijiaGate.nearGate(to)) {
            return;
        }
        Player player = event.getPlayer();
        World ameijia = Bukkit.getWorld(AmeijiaGate.WORLD_KEY);
        if (ameijia != null && player.getWorld().equals(ameijia)) {
            AmeijiaGate.toOverworld(player);
        } else {
            AmeijiaGate.toAmeijia(player);
        }
    }

    private static boolean movedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private void handle(PlayerTeleportEvent event, Player player) {
        World ameijia = Bukkit.getWorld(AmeijiaGate.WORLD_KEY);
        // アメイジア内 → 帰還。
        if (ameijia != null && player.getWorld().equals(ameijia)) {
            event.setCancelled(true);
            AmeijiaGate.toOverworld(player);
            return;
        }
        // 登録ゲート → アメイジアへ。
        if (AmeijiaGate.nearGate(event.getFrom())) {
            event.setCancelled(true);
            if (!AmeijiaGate.toAmeijia(player)) {
                player.sendMessage(Component.text(
                        "アメイジアが未ロードです（データパック配備後の再起動が必要かも）。", NamedTextColor.RED));
            }
        }
        // それ以外はバニラのゲートウェイ/ポータルなので干渉しない。
    }
}
