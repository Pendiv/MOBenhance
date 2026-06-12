package DIV.enhancedMobs.item;

import DIV.enhancedMobs.EnhancedMobs;
import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
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
 * 武器・防具強化エンジン。
 * <ul>
 *   <li>レベル: 1レベルにつき固定ステータス加算。レベルギャップ: 30（精錬&gt;=2 必要）、50（鍛造済み必要）、上限 70。</li>
 *   <li>精錬: 1精錬につきアイテム元ステータスの 20% を加算（精錬 5 = +100% = 2倍）。
 *       ネザライトは最大 10 まで可能; 精錬 10 で Lv70 ゲートが解除され上限が 100 になる。</li>
 *   <li>鍛造: メイスでアイテムを鍛造済みマーク（Lv50 ゲートを通過可能にする）。</li>
 * </ul>
 * ベースステータスはアイテム本来のデフォルトからモディファイアを再構築することで保持する。
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
    public static final NamespacedKey BROKEN = k("item_broken");

    /** 破壊寸前状態の解除に必要な耐久割合。 */
    public static final double BROKEN_CLEAR_RATIO = 0.75;
    private static final NamespacedKey M_ATK = k("ench_atk");
    private static final NamespacedKey M_SPD = k("ench_spd");
    private static final NamespacedKey M_ARM = k("ench_arm");
    private static final NamespacedKey M_TUF = k("ench_tuf");
    private static final NamespacedKey M_HP = k("skill_hp");

    public enum Category {
        WEAPON, ARMOR, TOOL
    }

    public static Category category(ItemStack item) {
        String n = item.getType().name();
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("MACE") || n.equals("TRIDENT")
                || n.endsWith("SPEAR")) {
            return Category.WEAPON;
        }
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || n.equals("ELYTRA")) {
            return Category.ARMOR;
        }
        // 道具ではピッケルのみフル強化対象。他の耐久品は isRepairOnly（耐久回復のみ）扱い。
        if (n.endsWith("_PICKAXE")) {
            return Category.TOOL;
        }
        return null;
    }

    /**
     * 修理のみ対応か: 強化カテゴリ外だが、修理素材が定義された耐久品
     * （シャベル・クワ・弓・クロスボウ・ハサミ・釣竿・盾等）。金床に載せて耐久回復だけ行える。
     */
    public static boolean isRepairOnly(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() == 1
                && category(item) == null
                && item.getType().getMaxDurability() > 0 && repairable(item) != null;
    }

    /** 金床の上に載せられるか（フル強化対象 or 修理のみ対応）。 */
    public static boolean isPlaceable(ItemStack item) {
        return isEnhanceable(item) || isRepairOnly(item);
    }

    /** アイテムの REPAIRABLE データコンポーネント（修理素材の定義）。未定義なら null。 */
    private static Repairable repairable(ItemStack item) {
        return item.getData(DataComponentTypes.REPAIRABLE);
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
        // メイスはネザライト同様 Lv100 まで（Lv70 ゲートは精錬5で解除）
        return isNetherite(item) || item.getType() == Material.MACE ? 100 : 70;
    }

    static int requiredXp(ItemStack item) {
        return Math.max(1, (int) Math.round(baseRequiredXp(item) * xpMultiplier()));
    }

    private static int baseRequiredXp(ItemStack item) {
        String n = item.getType().name();
        if (n.startsWith("WOODEN") || n.startsWith("LEATHER")) return 10;
        if (n.startsWith("STONE") || n.startsWith("CHAINMAIL")) return 15;
        if (n.startsWith("GOLDEN")) return 20;
        if (n.startsWith("IRON") || n.startsWith("TURTLE")) return 25;
        if (n.startsWith("DIAMOND")) return 50;
        if (n.startsWith("NETHERITE")) return 80;
        return 30;
    }

    /** レベルアップに必要な XP のコンフィグ倍率（1.0 = デフォルト）。 */
    private static double xpMultiplier() {
        EnhancedMobs plugin = EnhancedMobs.get();
        return plugin == null ? 1.0 : plugin.mainConfig().itemXpMultiplier;
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
            xp = req; // ゲートでブロック中はバーを満タンで停止
        }
        ItemSkills.rollIfNeeded(item, pdc, level); // Lv10 到達でレベリングスキル抽選

        pdc.set(LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(XP, PersistentDataType.INTEGER, xp);
        if (leveled) {
            applyStats(item, meta, level, refine);
        }
        updateLore(item, meta, level, xp, req, refine, forged);
        item.setItemMeta(meta);
    }

    /** 現在のレベルから次へ進めるかを、精錬・鍛造の進捗に基づいて判定。 */
    public static boolean canPass(ItemStack item, int level, int refine, int forged) {
        if (level >= cap(item)) return false;
        if (level == GATE_REFINE && refine < 2) return false;
        if (level == GATE_FORGE && forged < GATE_FORGE) return false;
        if (level == 70 && refine < maxRefine(item)) return false; // 精錬を極めると突破（ネザライト10 / メイス5。他素材は Lv70 が上限なので到達しない）
        return true;
    }

    /** 精錬 +1（上限: 通常 5、ネザライト 10）。 */
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

    /** 鍛造済みマークを付与（Lv50 ゲートを通過可能にする）。 */
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

    /** 耐久値を一部回復する（金床の基本機能）。修理のみ対応アイテムにも有効。 */
    public static boolean repair(ItemStack item) {
        if (!isPlaceable(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) return false;
        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        damageable.setDamage(Math.max(0, damageable.getDamage() - Math.max(1, max / 4)));
        item.setItemMeta(meta);
        clearBrokenIfRepaired(item);
        return true;
    }

    /**
     * レベルを直接設定して再構築する（OP コマンド用。ゲート・上限を無視するが cap では切る）。
     * 併せて XP を 0 にリセットし、Lv10 以上なら未抽選スキルの抽選も行う。
     */
    public static boolean setLevel(ItemStack item, int level) {
        if (!isEnhanceable(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var pdc = meta.getPersistentDataContainer();
        int clamped = Math.max(1, Math.min(level, cap(item)));
        pdc.set(LEVEL, PersistentDataType.INTEGER, clamped);
        pdc.set(XP, PersistentDataType.INTEGER, 0);
        ItemSkills.rollIfNeeded(item, pdc, clamped);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    /** 破壊寸前状態か。 */
    public static boolean isBroken(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(BROKEN, PersistentDataType.BYTE);
    }

    /** 破壊寸前状態を付与する（装備性能が 0 になる）。既に付与済みなら何もしない。 */
    public static void markBroken(ItemStack item) {
        if (!isEnhanceable(item) || isBroken(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(BROKEN, PersistentDataType.BYTE, (byte) 1);
        rebuild(item, meta);
        item.setItemMeta(meta);
    }

    /** 耐久が {@link #BROKEN_CLEAR_RATIO} 以上まで回復していれば破壊寸前状態を解除し、性能を復元する。 */
    public static boolean clearBrokenIfRepaired(ItemStack item) {
        if (!isBroken(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        if (max <= 0 || (max - damageable.getDamage()) / (double) max < BROKEN_CLEAR_RATIO) {
            return false;
        }
        meta.getPersistentDataContainer().remove(BROKEN);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    static void rebuild(ItemStack item, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        int level = pdc.getOrDefault(LEVEL, PersistentDataType.INTEGER, 1);
        int xp = pdc.getOrDefault(XP, PersistentDataType.INTEGER, 0);
        int refine = pdc.getOrDefault(REFINE, PersistentDataType.INTEGER, 0);
        int forged = pdc.getOrDefault(FORGED, PersistentDataType.INTEGER, 0);
        applyStats(item, meta, level, refine);
        updateLore(item, meta, level, xp, requiredXp(item), refine, forged);
    }

    /**
     * アイテム本来のデフォルトからモディファイアを再構築してベースステータスを保持したうえで、
     * レベルボーナス（固定値）と精錬ボーナス（元ステータスの 20% × 精錬数）を加算する。
     */
    public static void applyStats(ItemStack item, ItemMeta meta, int level, int refine) {
        Category cat = category(item);
        if (cat == null) {
            return;
        }
        EquipmentSlot slot = cat == Category.ARMOR ? armorSlot(item) : EquipmentSlot.HAND;
        EquipmentSlotGroup group = cat == Category.ARMOR ? armorGroup(item) : EquipmentSlotGroup.MAINHAND;

        Multimap<Attribute, AttributeModifier> defaults = item.getType().getDefaultAttributeModifiers(slot);
        Multimap<Attribute, AttributeModifier> currentMods = meta.getAttributeModifiers();
        if (currentMods != null) {
            for (Attribute attribute : new HashSet<>(currentMods.keySet())) {
                meta.removeAttributeModifier(attribute);
            }
        }
        boolean broken = meta.getPersistentDataContainer().has(BROKEN, PersistentDataType.BYTE);
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            AttributeModifier m = entry.getValue();
            // 破壊寸前は素のステータスも量 0 で明示上書き（モディファイア無しだとバニラ既定値に戻ってしまうため）
            meta.addAttributeModifier(entry.getKey(), broken
                    ? new AttributeModifier(m.getKey(), 0, m.getOperation(), m.getSlotGroup())
                    : m);
        }
        if (broken) {
            return; // 装備性能 0: レベル・精錬ボーナスも付与しない（耐久上限の拡張は現状維持）
        }

        double r = 0.2 * refine;
        if (cat == Category.WEAPON) {
            addBonus(meta, Attribute.ATTACK_DAMAGE, M_ATK,
                    level * 0.25 + r * baseStat(defaults, Attribute.ATTACK_DAMAGE, 1.0)
                            + ItemSkills.allInAttackBonus(meta, level), group);
            double allInSpeed = ItemSkills.allInSpeed(meta, level);
            if (allInSpeed >= 0) {
                // オールイン: 最終攻撃速度を固定値へ（既定込みの現在値との差分を加算。負値もあり得る）
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        M_SPD, allInSpeed - baseStat(defaults, Attribute.ATTACK_SPEED, 4.0),
                        AttributeModifier.Operation.ADD_NUMBER, group));
            } else if (item.getType() != Material.MACE) {
                // メイス例外: レベル・精錬とも攻撃力のみ上昇（攻撃速度はスマッシュ仕様を崩すため触らない）
                addBonus(meta, Attribute.ATTACK_SPEED, M_SPD,
                        level * 0.02 + r * baseStat(defaults, Attribute.ATTACK_SPEED, 4.0), group);
            }
        } else if (cat == Category.TOOL) {
            // 道具は耐久メイン + 控えめな攻撃力（武器の 0.25/Lv に対し 0.1/Lv）
            addBonus(meta, Attribute.ATTACK_DAMAGE, M_ATK,
                    level * 0.1 + r * baseStat(defaults, Attribute.ATTACK_DAMAGE, 1.0), group);
        } else {
            // 防具性能上昇スキル: 素の防具性能の 20/40/60/120% を加算（無効時 0）
            double boost = ItemSkills.armorBoostPct(meta, level);
            addBonus(meta, Attribute.ARMOR, M_ARM,
                    level * 0.25 + (r + boost) * baseStat(defaults, Attribute.ARMOR, 0.0), group);
            addBonus(meta, Attribute.ARMOR_TOUGHNESS, M_TUF,
                    level * 0.1 + (r + boost) * baseStat(defaults, Attribute.ARMOR_TOUGHNESS, 0.0), group);
        }
        // 最大体力増加スキル（防具のみ抽選されるが、判定はスキル有無で行う）
        addBonus(meta, Attribute.MAX_HEALTH, M_HP, ItemSkills.healthBonus(meta, level), group);
        applyDurability(item, meta, cat, level, r);
        ItemSkills.applyUnbreaking(meta, level);
    }

    /**
     * 耐久上限 = (素 + レベル/精錬補正) × 耐久強化スキル倍率。
     * 武器はレベル/精錬の耐久補正なし（スキル倍率のみ）。
     */
    private static void applyDurability(ItemStack item, ItemMeta meta, Category cat, int level, double r) {
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int base = item.getType().getMaxDurability();
        if (base <= 0) {
            return;
        }
        double withLevel = cat == Category.WEAPON ? base : base + level * 5 + r * base;
        double mult = ItemSkills.durabilityMultiplier(meta.getPersistentDataContainer(), level);
        int max = (int) Math.round(withLevel * mult);
        if (max != base || damageable.hasMaxDamage()) {
            damageable.setMaxDamage(max);
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
        lore.addAll(ItemSkills.loreLines(meta, level));
        if (meta.getPersistentDataContainer().has(BROKEN, PersistentDataType.BYTE)) {
            lore.add(Component.text("破壊寸前（性能0・要修理）", NamedTextColor.RED));
        }
        meta.lore(lore);
    }

    public static boolean isRepairMaterial(ItemStack item, Material held) {
        // バニラ定義（REPAIRABLE コンポーネント）があればそれを正とする
        Repairable rep = repairable(item);
        if (rep != null) {
            return rep.types().contains(TypedKey.create(RegistryKey.ITEM, held.getKey()));
        }
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
