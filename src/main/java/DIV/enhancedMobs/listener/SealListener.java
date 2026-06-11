package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.trait.seal.SealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;

/** RAGNAROK で封印されたアイテムを、プレイヤーがキャリアを「食べた」際に復元する。 */
public final class SealListener implements Listener {

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!SealUtil.isSealed(event.getItem())) {
            return;
        }
        event.setCancelled(true); // 金のリンゴ効果・通常消費を抑止する

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand() != null ? event.getHand() : EquipmentSlot.HAND;
        player.getInventory().setItem(hand, SealUtil.unseal(event.getItem()));
        player.sendMessage(Component.text("The seal is broken.", NamedTextColor.GREEN));
    }
}
