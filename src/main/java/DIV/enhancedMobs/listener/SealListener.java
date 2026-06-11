package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.trait.seal.SealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Restores a RAGNAROK-sealed item when the player finishes "eating" the carrier. */
public final class SealListener implements Listener {

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!SealUtil.isSealed(event.getItem())) {
            return;
        }
        event.setCancelled(true); // don't apply golden-apple effects or consume normally

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand() != null ? event.getHand() : EquipmentSlot.HAND;
        player.getInventory().setItem(hand, SealUtil.unseal(event.getItem()));
        player.sendMessage(Component.text("The seal is broken.", NamedTextColor.GREEN));
    }
}
