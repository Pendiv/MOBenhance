package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.world.AncientGate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 基幹ゲートの起動: リカバリーコンパスをフレーム底の強化深層岩に右クリックで使うと、
 * 魂の炎の演出が始まり END_PORTAL が出現する（{@link AncientGate}）。
 */
public final class AncientGateListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.REINFORCED_DEEPSLATE) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) {
            return;
        }
        event.setCancelled(true);
        if (AncientGate.tryIgnite(event.getPlayer(), block)) {
            event.getPlayer().sendMessage(Component.text("基幹ゲートが目を覚ます…", NamedTextColor.DARK_AQUA));
        }
    }
}
