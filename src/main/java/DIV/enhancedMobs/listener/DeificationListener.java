package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 神格化システム。最大段（4段）のビーコンの上面を、レベリング対象アイテムを持って右クリックすると
 * そのアイテムを神格化し、レベル上限を {@link ItemEnhancer#DEIFY_BONUS} 解放する
 * （ネザライト/メイス/盾 100→120、他 70→90）。スニーク時はバニラのビーコンGUIを開かせる。
 */
public final class DeificationListener implements Listener {

    @EventHandler
    public void onDeify(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON || event.getBlockFace() != BlockFace.UP) {
            return;
        }
        if (!(block.getState() instanceof Beacon beacon)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            return; // スニーク時はビーコンGUIを開かせる（神格化アイテム所持中のGUIアクセス手段）
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemEnhancer.isEnhanceable(held)) {
            return; // 対象外は通常操作（GUI）に委ねる
        }
        event.setCancelled(true);
        if (beacon.getTier() < 4) {
            Lang.actionbar(player, "emob.deify.need_max_beacon");
            return;
        }
        switch (ItemEnhancer.deify(held)) {
            case DEIFIED -> {
                player.getInventory().setItemInMainHand(held);
                celebrate(block);
                // 神格化はビーコンを消費する（終端エンドコンテンツの代償。ピラミッドは残る）。
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.6f);
                block.setType(Material.AIR);
                Lang.actionbar(player, "emob.deify.deified");
            }
            case ALREADY -> Lang.actionbar(player, "emob.deify.already");
            case NOT_APPLICABLE -> Lang.actionbar(player, "emob.deify.need_max_beacon");
        }
    }

    /** 神格化成功時の演出（ビーコン上面で発光・トーテム粒子・荘厳な音）。 */
    private void celebrate(Block beacon) {
        Location loc = beacon.getLocation().add(0.5, 1.2, 0.5);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 0.3, 0.5, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 0.4, 0.6, 0.4, 0.2);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
        loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 0.8f, 1.2f);
    }
}
