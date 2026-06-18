package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.grave.Grave;
import DIV.enhancedMobs.grave.GraveManager;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** 墓の生成（死亡）と、顕現中チェストの全方位保護。アイテム移動・破壊・爆発・ピストン・着火を遮断。 */
public final class GraveListener implements Listener {

    private static final String BYPASS = "enhancedmobs.grave.admin";

    private final GraveManager manager;

    public GraveListener(GraveManager manager) {
        this.manager = manager;
    }

    /**
     * 死亡時に墓を作る。{@code createGraveOnDeath} が drops から回収分だけ取り除く（残り＝損失分は
     * その場へ散らばる）。他プラグインの keepInventory/drops 変更を取り込んでから判定するため
     * HIGHEST（MONITOR より前）。
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return; // 本人が持つのでドロップ無し → 墓を作ると複製になる
        }
        manager.createGraveOnDeath(event.getEntity(), event.getDrops());
    }

    /** リスポーン時に墓の座標・距離を通知する。 */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        manager.onRespawn(event.getPlayer());
    }

    // ---- 顕現中チェストの保護 ----

    @EventHandler(ignoreCancelled = true)
    public void onBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Grave grave = manager.graveAt(event.getBlock());
        if (grave != null && !canEdit(event.getPlayer(), grave)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Grave grave = manager.graveAt(event.getClickedBlock());
        if (grave != null && !canEdit(event.getPlayer(), grave)) {
            event.setCancelled(true); // 他人の墓は開けない
        }
    }

    /** 顕現中チェストを手で空にしたら墓を撤去する。 */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Block block = chestBlock(event.getInventory());
        if (block != null) {
            manager.onChestEmptied(block);
        }
    }

    /** ホッパー等による吸い出し・流し込みを禁止。 */
    @EventHandler(ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (isGrave(event.getSource()) || isGrave(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (manager.graveAt(b) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (manager.graveAt(b) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> manager.graveAt(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> manager.graveAt(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (manager.graveAt(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (manager.graveAt(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    // ---- ヘルパ ----

    private boolean canEdit(Player player, Grave grave) {
        return grave.ownerId().equals(player.getUniqueId()) || player.hasPermission(BYPASS);
    }

    private boolean isGrave(Inventory inv) {
        Block block = chestBlock(inv);
        return block != null && manager.graveAt(block) != null;
    }

    /** インベントリの保持ブロック（チェスト/ダブルチェストの左側）を返す。墓判定に使う。 */
    private Block chestBlock(Inventory inv) {
        if (inv == null) {
            return null;
        }
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof DoubleChest dc && dc.getLeftSide() instanceof org.bukkit.block.Chest left) {
            return left.getBlock();
        }
        if (holder instanceof org.bukkit.block.Chest chest) {
            return chest.getBlock();
        }
        return null;
    }
}
