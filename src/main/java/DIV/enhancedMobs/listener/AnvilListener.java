package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Anvil-top item handling. Right-click an anvil's TOP face with a weapon/armor to set it on the
 * anvil (a paired ItemDisplay for visuals + an Interaction for the hitbox). Then right-click the
 * placed item with: the same item = refine, a repair material = restore durability, a mace = forge,
 * an empty hand = take it back. Entities are persistent so nothing is lost on unload/restart.
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
        if (!ItemEnhancer.isEnhanceable(held)) {
            return; // let the normal anvil GUI open
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
        if (held.getType() == placed.getType()) {
            if (ItemEnhancer.refine(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                msg(player, "精錬しました", NamedTextColor.GOLD);
            } else {
                msg(player, "これ以上精錬できません", NamedTextColor.RED);
            }
        } else if (held.getType() == Material.MACE) {
            if (ItemEnhancer.forge(placed)) {
                display.setItemStack(placed);
                msg(player, "鍛造しました", NamedTextColor.LIGHT_PURPLE);
            } else {
                msg(player, "既に鍛造済みです", NamedTextColor.RED);
            }
        } else if (ItemEnhancer.isRepairMaterial(placed, held.getType())) {
            if (ItemEnhancer.repair(placed)) {
                display.setItemStack(placed);
                consumeOne(player);
                msg(player, "耐久値を回復しました", NamedTextColor.GREEN);
            } else {
                msg(player, "耐久値は満タンです", NamedTextColor.GRAY);
            }
        }
    }

    private void place(Location loc, ItemStack item) {
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setPersistent(true);
            // Lay the item flat (parallel to the anvil top) and float it just above the surface.
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

    private void msg(Player player, String text, NamedTextColor color) {
        player.sendActionBar(Component.text(text, color));
    }

    private boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }
}
