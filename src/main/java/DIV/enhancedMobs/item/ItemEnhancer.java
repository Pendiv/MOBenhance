package DIV.enhancedMobs.item;

import DIV.enhancedMobs.EnhancedMobs;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Weapon/armor enhancement engine.
 * <ul>
 *   <li>Level: flat stat per level. Gates: 30 (needs refine&gt;=2), 50 (needs mace forge), 70 cap.</li>
 *   <li>Refine: each refine level adds 20% of the item's ORIGINAL stat (refine 5 = +100% = doubled).
 *       Netherite can refine up to 10; at refine 10 the Lv70 gate opens and the cap becomes 100.</li>
 *   <li>Forge: a mace marks the item forged (passes the Lv50 gate).</li>
 * </ul>
 * Base stats are preserved by rebuilding the item's modifier set from its intrinsic defaults.
 */
public final class ItemEnhancer {

    public static final int GATE_REFINE = 30;
    public static final int GATE_FORGE = 50;

    private ItemEnhancer() {
    }

    private static NamespacedKey k(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    public static final NamespacedKey LEVEL = k("item_level");
    public static final NamespacedKey XP = k("item_xp");
    public static final NamespacedKey REFINE = k("item_refine");
    public static final NamespacedKey FORGED = k("item_forged");
    private static final NamespacedKey M_ATK = k("ench_atk");
    private static final NamespacedKey M_SPD = k("ench_spd");
    private static final NamespacedKey M_ARM = k("ench_arm");
    private static final NamespacedKey M_TUF = k("ench_tuf");

    public enum Category {
        WEAPON, ARMOR
    }

    public static Category category(ItemStack item) {
        String n = item.getType().name();
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("MACE") || n.equals("TRIDENT")) {
            return Category.WEAPON;
        }
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || n.equals("ELYTRA")) {
            return Category.ARMOR;
        }
        return null;
    }

    public static boolean isEnhanceable(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() == 1 && category(item) != null;
    }

    private static boolean isNetherite(ItemStack item) {
        return item.getType().name().startsWith("NETHERITE");
    }

    public static int maxRefine(ItemStack item) {
        return isNetherite(item) ? 10 : 5;
    }

    public static int cap(ItemStack item) {
        return isNetherite(item) ? 100 : 70;
    }

    static int requiredXp(ItemStack item) {
        String n = item.getType().name();
        if (n.startsWith("WOODEN") || n.startsWith("LEATHER")) return 10;
        if (n.startsWith("STONE") || n.startsWith("CHAINMAIL")) return 15;
        if (n.startsWith("GOLDEN")) return 20;
        if (n.startsWith("IRON") || n.startsWith("TURTLE")) return 25;
        if (n.startsWith("DIAMOND")) return 50;
        if (n.startsWith("NETHERITE")) return 80;
        return 30;
    }

    public static void grantXp(ItemStack item, int amount) {
        if (!isEnhanceable(item) || amount <= 0) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        var pdc = meta.getPersistentDataContainer();
        int level = pdc.getOrDefault(LEVEL, PersistentDataType.INTEGER, 1);
        int xp = pdc.getOrDefault(XP, PersistentDataType.INTEGER, 0);
        int refine = pdc.getOrDefault(REFINE, PersistentDataType.INTEGER, 0);
        int forged = pdc.getOrDefault(FORGED, PersistentDataType.INTEGER, 0);
        if (level >= cap(item)) {
            return;
        }

        xp += amount;
        int req = requiredXp(item);
        boolean leveled = false;
        while (xp >= req && canPass(item, level, refine, forged)) {
            xp -= req;
            level++;
            leveled = true;
            if (level >= cap(item)) {
                break;
            }
        }
        if (!canPass(item, level, refine, forged) && xp > req) {
            xp = req; // park the bar at full while gated
        }

        pdc.set(LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(XP, PersistentDataType.INTEGER, xp);
        if (leveled) {
            applyStats(item, meta, level, refine);
        }
        updateLore(item, meta, level, xp, req, refine, forged);
        item.setItemMeta(meta);
    }

    /** Whether the item can level past its current level given its refine/forge progress. */
    public static boolean canPass(ItemStack item, int level, int refine, int forged) {
        if (level >= cap(item)) return false;
        if (level == GATE_REFINE && refine < 2) return false;
        if (level == GATE_FORGE && forged < GATE_FORGE) return false;
        if (level == 70 && refine < 10) return false; // netherite-only gate (others cap at 70)
        return true;
    }

    /** System 2: refine +1 (cap 5, or 10 for netherite). */
    public static boolean refine(ItemStack item) {
        if (!isEnhanceable(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        int refine = pdc.getOrDefault(REFINE, PersistentDataType.INTEGER, 0);
        if (refine >= maxRefine(item)) return false;
        pdc.set(REFINE, PersistentDataType.INTEGER, refine + 1);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    /** System 3: mark forged (passes the Lv50 gate). */
    public static boolean forge(ItemStack item) {
        if (!isEnhanceable(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        if (pdc.getOrDefault(FORGED, PersistentDataType.INTEGER, 0) >= GATE_FORGE) return false;
        pdc.set(FORGED, PersistentDataType.INTEGER, GATE_FORGE);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    /** Basic anvil-top function: restore some durability. */
    public static boolean repair(ItemStack item) {
        if (!isEnhanceable(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) return false;
        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        damageable.setDamage(Math.max(0, damageable.getDamage() - Math.max(1, max / 4)));
        item.setItemMeta(meta);
        return true;
    }

    private static void rebuild(ItemStack item, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        int level = pdc.getOrDefault(LEVEL, PersistentDataType.INTEGER, 1);
        int xp = pdc.getOrDefault(XP, PersistentDataType.INTEGER, 0);
        int refine = pdc.getOrDefault(REFINE, PersistentDataType.INTEGER, 0);
        int forged = pdc.getOrDefault(FORGED, PersistentDataType.INTEGER, 0);
        applyStats(item, meta, level, refine);
        updateLore(item, meta, level, xp, requiredXp(item), refine, forged);
    }

    /**
     * Rebuild the item's modifiers from its intrinsic defaults (preserving base stats), then add the
     * level bonus (flat) and the refine bonus (20% of the original stat per refine level).
     */
    public static void applyStats(ItemStack item, ItemMeta meta, int level, int refine) {
        Category cat = category(item);
        if (cat == null) {
            return;
        }
        EquipmentSlot slot = cat == Category.WEAPON ? EquipmentSlot.HAND : armorSlot(item);
        EquipmentSlotGroup group = cat == Category.WEAPON ? EquipmentSlotGroup.MAINHAND : armorGroup(item);

        Multimap<Attribute, AttributeModifier> defaults = item.getType().getDefaultAttributeModifiers(slot);
        Multimap<Attribute, AttributeModifier> currentMods = meta.getAttributeModifiers();
        if (currentMods != null) {
            for (Attribute attribute : new HashSet<>(currentMods.keySet())) {
                meta.removeAttributeModifier(attribute);
            }
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            meta.addAttributeModifier(entry.getKey(), entry.getValue());
        }

        double r = 0.2 * refine;
        if (cat == Category.WEAPON) {
            addBonus(meta, Attribute.ATTACK_DAMAGE, M_ATK,
                    level * 0.25 + r * baseStat(defaults, Attribute.ATTACK_DAMAGE, 1.0), group);
            addBonus(meta, Attribute.ATTACK_SPEED, M_SPD,
                    level * 0.02 + r * baseStat(defaults, Attribute.ATTACK_SPEED, 4.0), group);
        } else {
            addBonus(meta, Attribute.ARMOR, M_ARM,
                    level * 0.25 + r * baseStat(defaults, Attribute.ARMOR, 0.0), group);
            addBonus(meta, Attribute.ARMOR_TOUGHNESS, M_TUF,
                    level * 0.1 + r * baseStat(defaults, Attribute.ARMOR_TOUGHNESS, 0.0), group);
            if (meta instanceof Damageable damageable) {
                int base = item.getType().getMaxDurability();
                if (base > 0) {
                    damageable.setMaxDamage(base + level * 5 + (int) (r * base));
                }
            }
        }
    }

    private static double baseStat(Multimap<Attribute, AttributeModifier> defaults, Attribute attr, double generic) {
        double sum = generic;
        for (AttributeModifier m : defaults.get(attr)) {
            sum += m.getAmount();
        }
        return sum;
    }

    private static void addBonus(ItemMeta meta, Attribute attribute, NamespacedKey key, double amount,
                                 EquipmentSlotGroup group) {
        if (amount > 0) {
            meta.addAttributeModifier(attribute,
                    new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER, group));
        }
    }

    private static EquipmentSlot armorSlot(ItemStack item) {
        String n = item.getType().name();
        if (n.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (n.endsWith("_CHESTPLATE") || n.equals("ELYTRA")) return EquipmentSlot.CHEST;
        if (n.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        return EquipmentSlot.FEET;
    }

    private static EquipmentSlotGroup armorGroup(ItemStack item) {
        String n = item.getType().name();
        if (n.endsWith("_HELMET")) return EquipmentSlotGroup.HEAD;
        if (n.endsWith("_CHESTPLATE") || n.equals("ELYTRA")) return EquipmentSlotGroup.CHEST;
        if (n.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        return EquipmentSlotGroup.FEET;
    }

    private static void updateLore(ItemStack item, ItemMeta meta, int level, int xp, int req, int refine, int forged) {
        List<Component> lore = new ArrayList<>();
        String progress = level >= cap(item) ? " (MAX)" : "  " + xp + "/" + req;
        lore.add(Component.text("強化 Lv." + level + progress, NamedTextColor.AQUA));
        if (refine > 0) {
            lore.add(Component.text("精錬 +" + refine + "/" + maxRefine(item), NamedTextColor.GOLD));
        }
        if (forged >= GATE_FORGE) {
            lore.add(Component.text("鍛造済", NamedTextColor.LIGHT_PURPLE));
        }
        meta.lore(lore);
    }

    public static boolean isRepairMaterial(ItemStack item, Material held) {
        String n = item.getType().name();
        if (n.startsWith("WOODEN")) return held.name().endsWith("_PLANKS");
        if (n.startsWith("STONE")) return held == Material.COBBLESTONE;
        if (n.startsWith("IRON") || n.startsWith("CHAINMAIL")) return held == Material.IRON_INGOT;
        if (n.startsWith("GOLDEN")) return held == Material.GOLD_INGOT;
        if (n.startsWith("DIAMOND")) return held == Material.DIAMOND;
        if (n.startsWith("NETHERITE")) return held == Material.NETHERITE_INGOT;
        if (n.startsWith("LEATHER")) return held == Material.LEATHER;
        if (n.startsWith("TURTLE")) return held == Material.TURTLE_SCUTE;
        return false;
    }
}
