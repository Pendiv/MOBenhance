package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
 * 金床上面へのアイテム設置と操作を管理する。
 * 武器/防具を持って金床の上面を右クリックすると設置（視覚用 ItemDisplay + ヒットボックス用 Interaction を生成）。
 * 設置済みアイテムを右クリックすると：同種アイテム=精錬、修理素材=耐久回復、メイス=鍛造、空手=回収。
 * エンティティは永続化されるためアンロード・再起動でも消えない。
 * 強化済の武器でも精錬できてしまう問題はどうしたものかと思案中
 */
public final class AnvilListener implements Listener {

    private static NamespacedKey k(String n) {
        return new NamespacedKey(EnhancedMobs.get(), n);
    }

    private static final NamespacedKey TAG_INTER = k("anvil_inter");
    private static final NamespacedKey TAG_DISPLAY = k("anvil_display");
    private static final NamespacedKey DISPLAY_UUID = k("anvil_display_uuid");

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isAnvil(block.getType()) || event.getBlockFace() != BlockFace.UP) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemEnhancer.isPlaceable(held)) {
            return; // 対象外なら通常の金床GUIを開かせる
        }
        Location loc = block.getLocation().add(0.5, 1.0, 0.5);
        event.setCancelled(true);
        if (occupied(loc)) {
            return;
        }
        place(loc, held.clone());
        player.getInventory().setItemInMainHand(null);
    }

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
        Player player = event.getPlayer();
        ItemStack placed = display.getItemStack();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (held == null || held.getType() == Material.AIR) {
            giveOrDrop(player, placed);
            display.remove();
            inter.remove();
            return;
        }
        boolean repairOnly = !ItemEnhancer.isEnhanceable(placed);
        if (ItemEnhancer.isRepairMaterial(placed, held.getType())) {
            if (ItemEnhancer.repair(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                if (ItemEnhancer.isBroken(placed)) {
                    Lang.actionbar(player, "emob.anvil.repaired_broken");
                } else {
                    Lang.actionbar(player, "emob.anvil.repaired");
                }
            } else {
                Lang.actionbar(player, "emob.anvil.repair_full");
            }
        } else if (ItemSkills.isBonusItem(held)) {
            switch (ItemSkills.grantBonus(placed, held)) {
                case GRANTED -> {
                    display.setItemStack(placed);
                    consumeOne(player);
                    Lang.actionbar(player, "emob.anvil.bonus_granted",
                            Component.text(ItemSkills.bonusDisplayName(held)));
                }
                case ALREADY -> Lang.actionbar(player, "emob.anvil.bonus_already");
                case NOT_APPLICABLE -> Lang.actionbar(player, "emob.anvil.bonus_not_applicable");
            }
        } else if (held.getType() == Material.WITHER_SKELETON_SKULL) {
            if (ItemSkills.rerollSkill(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                Lang.actionbar(player, "emob.anvil.skill_rerolled",
                        Component.text(ItemSkills.skillDisplayName(ItemSkills.skillId(placed))));
            } else {
                Lang.actionbar(player, "emob.anvil.reroll_none");
            }
        } else if (held.getType() == Material.DRAGON_HEAD) {
            // ドラゴンの頭: レベリング対象なら種別を問わず即座にレベル・精錬・鍛造・鋳造MAX。
            if (ItemEnhancer.maxOut(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                Lang.actionbar(player, "emob.anvil.dragon_maxed");
            } else {
                Lang.actionbar(player, "emob.anvil.dragon_fail");
            }
        } else if (repairOnly) {
            Lang.actionbar(player, "emob.anvil.repair_only");
        } else if (held.getType() == Material.MACE
                && !(placed.getType() == Material.MACE && player.isSneaking())) {
            // 鍛造を精錬より優先（メイス×メイスの誤消費防止）。メイス同士の精錬はスニーククリックで行う。
            if (ItemEnhancer.forge(placed)) {
                display.setItemStack(placed);
                Lang.actionbar(player, "emob.anvil.forged");
            } else if (placed.getType() == Material.MACE) {
                Lang.actionbar(player, "emob.anvil.forge_already_mace");
            } else {
                Lang.actionbar(player, "emob.anvil.forge_already");
            }
        } else if (held.getType() == placed.getType()) {
            if (ItemEnhancer.refine(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                Lang.actionbar(player, "emob.anvil.refined");
            } else {
                Lang.actionbar(player, "emob.anvil.refine_max");
            }
        }
    }

    private void place(Location loc, ItemStack item) {
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setPersistent(true);
            // アイテムを金床の天面に平行に寝かせ、表面のすぐ上に浮かせる。
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
            i.getPersistentDataContainer().set(DISPLAY_UUID, PersistentDataType.STRING, display.getUniqueId().toString()); // Interaction から Display を逆引きするために UUID を保持
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

    private boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }
}
