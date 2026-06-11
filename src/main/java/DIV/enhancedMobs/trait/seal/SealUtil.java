package DIV.enhancedMobs.trait.seal;

import DIV.enhancedMobs.EnhancedMobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Base64;

/**
 * Item sealing for RAGNAROK: stores an item's full bytes inside an always-edible carrier
 * (golden apple). Eating it restores the original.
 */
public final class SealUtil {

    public static final NamespacedKey SEAL_KEY = new NamespacedKey(EnhancedMobs.get(), "sealed_item");

    private SealUtil() {
    }

    public static ItemStack seal(ItemStack original) {
        ItemStack carrier = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = carrier.getItemMeta();
        meta.displayName(Component.text("Sealed Item", NamedTextColor.LIGHT_PURPLE));
        String encoded = Base64.getEncoder().encodeToString(original.serializeAsBytes());
        meta.getPersistentDataContainer().set(SEAL_KEY, PersistentDataType.STRING, encoded);
        carrier.setItemMeta(meta);
        return carrier;
    }

    public static boolean isSealed(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(SEAL_KEY, PersistentDataType.STRING);
    }

    public static ItemStack unseal(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String encoded = meta.getPersistentDataContainer().get(SEAL_KEY, PersistentDataType.STRING);
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }
}
