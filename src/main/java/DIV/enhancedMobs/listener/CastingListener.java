package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 鋳造（casting）システム。現世の満チャージのリスポーンアンカーに、鋳造系アイテム（弓/盾）を
 * <b>ドロップ（Q）して載せる</b>（弓のドロー/盾の構え/アンカー爆発と競合しないようドロップで設置）。
 * 載った状態でアンカー（または載ったアイテム）を素手で右クリックするたびにチャージを 1 消費して 1 鋳造
 * （{@link ItemEnhancer#CAST_REQUIRED} 回で Lv50 ゲート解放）。グロウストーンで再チャージ、スニーク素手で回収。
 * アイテムが載っている間はアンカーのクリックを横取りするため現世でも爆発しない。
 */
public final class CastingListener implements Listener {

    private static NamespacedKey k(String n) {
        return new NamespacedKey(EnhancedMobs.get(), n);
    }

    private static final NamespacedKey TAG_INTER = k("cast_inter");
    private static final NamespacedKey TAG_DISPLAY = k("cast_display");
    private static final NamespacedKey DISPLAY_UUID = k("cast_display_uuid");

    /** 鋳造系アイテムを満チャージアンカーへドロップして載せる。 */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        ItemStack item = drop.getItemStack();
        if (!ItemEnhancer.isEnhanceable(item) || !ItemEnhancer.usesCasting(item)) {
            return;
        }
        Player player = event.getPlayer();
        Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.RESPAWN_ANCHOR
                || target.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return; // アンカーを見ていなければ通常ドロップ
        }
        if (!(target.getBlockData() instanceof RespawnAnchor anchor)) {
            return;
        }
        if (anchor.getCharges() < anchor.getMaximumCharges()) {
            event.setCancelled(true); // 満チャージでなければ載せず手元に戻す
            Lang.actionbar(player, "emob.cast.need_full");
            return;
        }
        Location loc = target.getLocation().add(0.5, 1.0, 0.5);
        if (occupied(loc)) {
            event.setCancelled(true);
            return; // 既に何か載っている
        }
        // ドロップは消費してアンカー上に設置（複製・紛失なし）。
        place(loc, item.asOne());
        drop.remove();
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.4f);
    }

    /** 載ったアイテム（Interaction）への右クリック。 */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Interaction inter)) {
            return;
        }
        if (!inter.getPersistentDataContainer().has(TAG_INTER, PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        ItemDisplay display = display(inter);
        if (display == null) {
            inter.remove();
            return;
        }
        operate(event.getPlayer(), display, inter, anchorBelow(display));
    }

    /** アイテムが載ったアンカーへの右クリックを横取り（現世の爆発防止）し、同じ操作にまわす。 */
    @EventHandler
    public void onAnchorClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR) {
            return;
        }
        Interaction inter = interOn(block);
        if (inter == null) {
            return; // 何も載っていない素のアンカーはバニラ挙動に委ねる
        }
        event.setCancelled(true); // 載っている間は爆発させない
        ItemDisplay display = display(inter);
        if (display == null) {
            inter.remove();
            return;
        }
        operate(event.getPlayer(), display, inter, block);
    }

    /** 鋳造・再チャージ・回収の共通処理。 */
    private void operate(Player player, ItemDisplay display, Interaction inter, Block anchorBlock) {
        ItemStack placed = display.getItemStack();
        Location loc = display.getLocation();
        ItemStack held = player.getInventory().getItemInMainHand();
        boolean empty = held == null || held.getType() == Material.AIR;

        // 素手スニーク = 回収
        if (empty && player.isSneaking()) {
            giveOrDrop(player, placed);
            display.remove();
            inter.remove();
            loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 1.3f);
            return;
        }
        if (anchorBlock == null || anchorBlock.getType() != Material.RESPAWN_ANCHOR
                || !(anchorBlock.getBlockData() instanceof RespawnAnchor anchor)) {
            // 台座が無くなっていたら回収して終了
            giveOrDrop(player, placed);
            display.remove();
            inter.remove();
            return;
        }

        // グロウストーンで再チャージ
        if (!empty && held.getType() == Material.GLOWSTONE) {
            if (anchor.getCharges() >= anchor.getMaximumCharges()) {
                Lang.actionbar(player, "emob.cast.charge_full");
                return;
            }
            anchor.setCharges(anchor.getCharges() + 1);
            anchorBlock.setBlockData(anchor);
            consumeOne(player);
            loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.0f);
            return;
        }

        // 残響の欠片で鋳造上限を 8→16 解放
        if (!empty && held.getType() == Material.ECHO_SHARD) {
            switch (ItemEnhancer.extendCast(placed)) {
                case EXTENDED -> {
                    display.setItemStack(placed);
                    consumeOne(player);
                    loc.getWorld().playSound(loc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.8f);
                    loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 30, 0.3, 0.4, 0.3, 0.02);
                    Lang.actionbar(player, "emob.cast.extended");
                }
                case NEED_CASTS -> Lang.actionbar(player, "emob.cast.extend_need_casts",
                        Component.text(ItemEnhancer.CAST_GATE_50));
                case ALREADY -> Lang.actionbar(player, "emob.cast.extend_already");
                case NOT_APPLICABLE -> { }
            }
            return;
        }

        // 素手 = 鋳造（チャージ -1）
        if (empty) {
            if (anchor.getCharges() <= 0) {
                Lang.actionbar(player, "emob.cast.no_charge");
                return;
            }
            if (!ItemEnhancer.cast(placed)) {
                // 現在の上限に到達：未解放なら残響の欠片を促し、解放済みなら完了
                Lang.actionbar(player, ItemEnhancer.isCastExtended(placed)
                        ? "emob.cast.complete" : "emob.cast.ready_shard");
                return;
            }
            anchor.setCharges(anchor.getCharges() - 1);
            anchorBlock.setBlockData(anchor);
            display.setItemStack(placed);
            int count = ItemEnhancer.castCount(placed);
            int cap = ItemEnhancer.castCap(placed);
            celebrate(loc);
            if (count >= cap) {
                Lang.actionbar(player, cap == ItemEnhancer.CAST_GATE_50
                        ? "emob.cast.ready_shard" : "emob.cast.complete");
            } else {
                Lang.actionbar(player, "emob.cast.progress", Component.text(count), Component.text(cap));
            }
        }
    }

    private void celebrate(Location loc) {
        World w = loc.getWorld();
        w.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.9f, 0.8f);
        w.playSound(loc, Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 24, 0.25, 0.35, 0.25, 0.02);
        w.spawnParticle(Particle.REVERSE_PORTAL, loc, 30, 0.3, 0.4, 0.3, 0.05);
    }

    /** 載ったアイテムの真下のアンカーブロック。 */
    private Block anchorBelow(ItemDisplay display) {
        return display.getLocation().clone().subtract(0.5, 1.0, 0.5).getBlock();
    }

    /** アンカー上面に載っている casting の Interaction（無ければ null）。 */
    private Interaction interOn(Block anchor) {
        Location top = anchor.getLocation().add(0.5, 1.0, 0.5);
        for (Entity e : anchor.getWorld().getNearbyEntities(top, 0.5, 0.6, 0.5)) {
            if (e instanceof Interaction inter
                    && inter.getPersistentDataContainer().has(TAG_INTER, PersistentDataType.BYTE)) {
                return inter;
            }
        }
        return null;
    }

    private void place(Location loc, ItemStack item) {
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setPersistent(true);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.08f, 0f),
                    new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()));
            d.getPersistentDataContainer().set(TAG_DISPLAY, PersistentDataType.BYTE, (byte) 1);
        });
        loc.getWorld().spawn(loc, Interaction.class, i -> {
            i.setInteractionWidth(0.6f);
            i.setInteractionHeight(0.6f);
            i.setResponsive(true);
            i.setPersistent(true);
            i.getPersistentDataContainer().set(TAG_INTER, PersistentDataType.BYTE, (byte) 1);
            i.getPersistentDataContainer().set(DISPLAY_UUID, PersistentDataType.STRING, display.getUniqueId().toString());
        });
    }

    private boolean occupied(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.5, 0.6, 0.5)) {
            if (e instanceof Interaction && e.getPersistentDataContainer().has(TAG_INTER, PersistentDataType.BYTE)) {
                return true;
            }
        }
        return false;
    }

    private ItemDisplay display(Interaction inter) {
        String uuid = inter.getPersistentDataContainer().get(DISPLAY_UUID, PersistentDataType.STRING);
        if (uuid == null) {
            return null;
        }
        Entity e = Bukkit.getEntity(UUID.fromString(uuid));
        return e instanceof ItemDisplay display ? display : null;
    }

    private void consumeOne(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        held.setAmount(held.getAmount() - 1);
        player.getInventory().setItemInMainHand(held.getAmount() <= 0 ? null : held);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
