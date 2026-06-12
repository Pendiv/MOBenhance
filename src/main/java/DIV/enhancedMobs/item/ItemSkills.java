package DIV.enhancedMobs.item;

import DIV.enhancedMobs.EnhancedMobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * レベリングスキルと付加スキル。
 * <ul>
 *   <li><b>レベリングスキル</b>: Lv10 で抽選プールから1つ決定、Lv30 で有効化、
 *       Lv50/70/100 で各1回強化（強化段階 0〜3）。現状のプール: 耐久強化。</li>
 *   <li><b>付加スキル</b>: 金床に置いたアイテムへ要求アイテムを右クリックして付与する。
 *       現状: 暗視（ヘルメット限定、要求 = 8分暗視ポーション）。</li>
 * </ul>
 */
public final class ItemSkills {

    private ItemSkills() {
    }

    private static NamespacedKey k(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    /** 抽選で決まったレベリングスキルの id。 */
    public static final NamespacedKey SKILL = k("item_skill");
    /** 耐久強化: スキル適用前の素の耐久エンチャレベル（冪等な再適用のため保存）。 */
    public static final NamespacedKey UNBR_BASE = k("item_skill_unbr_base");
    /** 付加スキル id のカンマ区切りリスト。 */
    public static final NamespacedKey BONUS = k("item_bonus_skills");

    public static final int ROLL_LEVEL = 10;
    public static final int ACTIVE_LEVEL = 30;
    private static final int[] UPGRADE_LEVELS = {50, 70, 100};

    public static final String SKILL_DURABILITY = "durability";
    public static final String SKILL_FLYING = "flying";
    public static final String SKILL_HEALTH = "max_health";
    public static final String SKILL_CHARGE = "charge_relief";
    public static final String SKILL_VALOR = "valor";
    public static final String SKILL_DASH = "dash";
    public static final String SKILL_ALL_IN = "all_in";
    public static final String SKILL_JUST_BLOCK = "just_block";
    public static final String SKILL_TAKENOKO = "takenoko";
    private static final String BONUS_NIGHT_VISION = "night_vision";
    public static final String BONUS_AUTO_MACE = "auto_mace";
    public static final String BONUS_ATTACK_LINGER = "attack_linger";

    /** 設定可能なスキル id 一覧（コマンドのバリデーション・補完用）。 */
    public static final List<String> SKILL_IDS = List.of(
            SKILL_DURABILITY, SKILL_FLYING, SKILL_HEALTH, SKILL_CHARGE, SKILL_VALOR, SKILL_DASH,
            SKILL_ALL_IN, SKILL_JUST_BLOCK, SKILL_TAKENOKO);

    /** 強化段階ごとの耐久上限倍率（未強化/+1/+2/+3）。 */
    private static final double[] DURA_MULT = {1.2, 1.4, 1.6, 2.2};
    /** 強化段階ごとの耐久エンチャ加算（未強化/+1/+2/+3）。 */
    private static final int[] UNBR_BONUS = {1, 1, 2, 3};
    /** 飛翔: 強化段階ごとのクールタイム（秒）。 */
    public static final int[] FLYING_COOLDOWN_SEC = {9, 8, 7, 3};
    /** 飛翔: 強化段階ごとの飛翔可能範囲（ブロック）。 */
    public static final double[] FLYING_RANGE = {12, 14, 16, 99};
    /** オートメイス: 強化段階ごとのクールタイム（秒）。 */
    public static final int[] AUTO_MACE_COOLDOWN_SEC = {10, 9, 8, 2};
    /** オートメイス: 強化段階ごとの上昇ブロック数。 */
    public static final double[] AUTO_MACE_RISE = {10, 12, 14, 25};
    /** 最大体力増加: 段階ごとの体力加算。 */
    static final double[] HEALTH_BONUS = {2, 3, 4, 8};
    /** 突進軽減: 段階ごとの満腹度回復量。 */
    public static final int[] CHARGE_HUNGER = {2, 4, 6, 20};
    /** 突進軽減: 突進後の落下ダメージ無効時間（tick）。 */
    public static final int CHARGE_FALL_IMMUNE_TICKS = 100;
    /** 勇猛果敢: 段階ごとの効果レベル上限。 */
    public static final int[] VALOR_CAP = {5, 10, 20, 99};
    /** 勇猛果敢: 段階ごとの効果時間（秒）。 */
    public static final int[] VALOR_DURATION_SEC = {5, 5, 6, 10};
    /** 突飛: 段階ごとのクールタイム（tick）。CT 5 / 4.5 / 4 / 1.5 秒。 */
    public static final int[] DASH_COOLDOWN_TICKS = {100, 90, 80, 30};
    /** 突飛: 段階ごとの突進初速（飛距離が段階で伸びる）。 */
    public static final double[] DASH_POWER = {1.8, 2.2, 2.6, 3.5};
    /** オールイン: 段階ごとの固定攻撃速度。 */
    static final double[] ALL_IN_SPEED = {0.6, 0.6, 0.7, 0.9};
    /** オールイン: 段階ごとの攻撃力加算。 */
    static final double[] ALL_IN_ATK = {7, 10, 13, 25};
    /** ジャストブロック: 段階ごとのブロック確率。複数部位でも最大段階のみ有効（非累積）。 */
    public static final double[] JUST_BLOCK_CHANCE = {0.25, 0.30, 0.35, 0.50};
    /** たけのこ魔法: 段階ごとの打ち上げ高さ（ブロック）。 */
    public static final double[] TAKENOKO_HEIGHT = {6, 8, 10, 16};
    /** 攻撃滞留: 段階ごとの発生確率（「稀に」）。 */
    public static final double[] LINGER_CHANCE = {0.10, 0.15, 0.20, 0.30};

    // ---- レベリングスキル ----

    /** Lv10 到達時の抽選。未抽選なら、そのアイテムに適合するプールから1つ決める。 */
    static void rollIfNeeded(ItemStack item, PersistentDataContainer pdc, int level) {
        if (level < ROLL_LEVEL || pdc.has(SKILL, PersistentDataType.STRING)) {
            return;
        }
        List<String> pool = new ArrayList<>();
        pool.add(SKILL_DURABILITY);
        String n = item.getType().name();
        if (n.endsWith("_AXE")) {
            pool.add(SKILL_FLYING); // 斧限定
            pool.add(SKILL_ALL_IN);
            pool.add(SKILL_TAKENOKO);
        }
        if (n.endsWith("_SWORD")) {
            pool.add(SKILL_VALOR); // 剣限定
            pool.add(SKILL_DASH);
        }
        if (n.endsWith("SPEAR")) {
            pool.add(SKILL_CHARGE); // 槍限定
        }
        if (ItemEnhancer.category(item) == ItemEnhancer.Category.ARMOR) {
            pool.add(SKILL_HEALTH); // 防具限定
            pool.add(SKILL_JUST_BLOCK);
        }
        pdc.set(SKILL, PersistentDataType.STRING, pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }

    /** 強化段階: -1=未抽選または Lv30 未満（無効）、0=有効（未強化）、1〜3=強化回数。 */
    static int stage(PersistentDataContainer pdc, int level) {
        if (!pdc.has(SKILL, PersistentDataType.STRING) || level < ACTIVE_LEVEL) {
            return -1;
        }
        int s = 0;
        for (int threshold : UPGRADE_LEVELS) {
            if (level >= threshold) {
                s++;
            }
        }
        return s;
    }

    /** 耐久強化スキルによる耐久上限倍率（無効時は 1.0）。 */
    static double durabilityMultiplier(PersistentDataContainer pdc, int level) {
        int s = stage(pdc, level);
        return s >= 0 && SKILL_DURABILITY.equals(pdc.get(SKILL, PersistentDataType.STRING))
                ? DURA_MULT[s] : 1.0;
    }

    /**
     * 耐久強化スキル: 耐久エンチャントが付いている場合のみ、素レベル + 段階ボーナスへ引き上げる。
     * 素レベルは初回適用時に PDC へ保存し、再適用（rebuild）で累積しないようにする。
     */
    static void applyUnbreaking(ItemMeta meta, int level) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int s = stage(pdc, level);
        if (s < 0 || !SKILL_DURABILITY.equals(pdc.get(SKILL, PersistentDataType.STRING))) {
            return;
        }
        Integer base = pdc.get(UNBR_BASE, PersistentDataType.INTEGER);
        if (base == null) {
            if (!meta.hasEnchant(Enchantment.UNBREAKING)) {
                return; // 耐久エンチャ無しならボーナスも無し
            }
            base = meta.getEnchantLevel(Enchantment.UNBREAKING);
            pdc.set(UNBR_BASE, PersistentDataType.INTEGER, base);
        }
        meta.addEnchant(Enchantment.UNBREAKING, base + UNBR_BONUS[s], true);
    }

    // ---- lore ----

    /** スキル・付加スキルの lore 行（updateLore から呼ばれる）。 */
    static List<Component> loreLines(ItemMeta meta, int level) {
        List<Component> lines = new ArrayList<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(SKILL, PersistentDataType.STRING);
        if (id != null) {
            int s = stage(pdc, level);
            if (s < 0) {
                lines.add(Component.text("スキル: " + skillName(id) + "（Lv" + ACTIVE_LEVEL + "で有効化）",
                        NamedTextColor.DARK_GRAY));
            } else {
                String suffix = s == 0 ? "" : " +" + s;
                lines.add(Component.text("スキル: " + skillName(id) + suffix, NamedTextColor.GREEN));
            }
        }
        for (String bonus : bonusList(pdc)) {
            lines.add(Component.text("付加: " + bonusName(bonus), NamedTextColor.LIGHT_PURPLE));
        }
        return lines;
    }

    private static String skillName(String id) {
        return switch (id) {
            case SKILL_DURABILITY -> "耐久強化";
            case SKILL_FLYING -> "飛翔";
            case SKILL_HEALTH -> "最大体力増加";
            case SKILL_CHARGE -> "突進軽減";
            case SKILL_VALOR -> "勇猛果敢";
            case SKILL_DASH -> "突飛";
            case SKILL_ALL_IN -> "オールイン";
            case SKILL_JUST_BLOCK -> "ジャストブロック";
            case SKILL_TAKENOKO -> "たけのこ魔法";
            default -> id;
        };
    }

    /** 指定スキルが有効な場合の強化段階（ItemMeta 版）。不一致・無効なら -1。 */
    private static int activeStageOf(ItemMeta meta, int level, String id) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!id.equals(pdc.get(SKILL, PersistentDataType.STRING))) {
            return -1;
        }
        return stage(pdc, level);
    }

    /** 最大体力増加スキルによる体力加算（無効時は 0）。ItemEnhancer.applyStats から呼ばれる。 */
    static double healthBonus(ItemMeta meta, int level) {
        int s = activeStageOf(meta, level, SKILL_HEALTH);
        return s < 0 ? 0 : HEALTH_BONUS[s];
    }

    /** オールインの攻撃力加算（無効時は 0）。 */
    static double allInAttackBonus(ItemMeta meta, int level) {
        int s = activeStageOf(meta, level, SKILL_ALL_IN);
        return s < 0 ? 0 : ALL_IN_ATK[s];
    }

    /** オールインの固定攻撃速度（無効時は -1）。 */
    static double allInSpeed(ItemMeta meta, int level) {
        int s = activeStageOf(meta, level, SKILL_ALL_IN);
        return s < 0 ? -1 : ALL_IN_SPEED[s];
    }

    // ---- 外部 API（リスナー・コマンド用） ----

    /** アイテムのレベリングスキル id（未抽選なら null）。 */
    public static String skillId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(SKILL, PersistentDataType.STRING);
    }

    /** 指定スキルが有効な場合の強化段階（0〜3）。スキル不一致・無効（Lv30未満）なら -1。 */
    public static int activeStage(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!id.equals(pdc.get(SKILL, PersistentDataType.STRING))) {
            return -1;
        }
        return stage(pdc, pdc.getOrDefault(ItemEnhancer.LEVEL, PersistentDataType.INTEGER, 1));
    }

    /**
     * レベリングスキルを直接設定する（コマンド用）。id = "none" で削除。
     * 耐久強化が適用済みだった場合は耐久エンチャを素レベルへ戻す。
     */
    public static boolean setSkill(ItemStack item, String id) {
        if (!ItemEnhancer.isEnhanceable(item) || (!"none".equals(id) && !SKILL_IDS.contains(id))) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer unbrBase = pdc.get(UNBR_BASE, PersistentDataType.INTEGER);
        if (unbrBase != null) {
            meta.addEnchant(Enchantment.UNBREAKING, unbrBase, true);
            pdc.remove(UNBR_BASE);
        }
        if ("none".equals(id)) {
            pdc.remove(SKILL);
        } else {
            pdc.set(SKILL, PersistentDataType.STRING, id);
        }
        ItemEnhancer.rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    private static String bonusName(String id) {
        return switch (id) {
            case BONUS_NIGHT_VISION -> "暗視";
            case BONUS_AUTO_MACE -> "オートメイス";
            case BONUS_ATTACK_LINGER -> "攻撃滞留";
            default -> id;
        };
    }

    // ---- 付加スキル ----

    public enum BonusResult {
        GRANTED, ALREADY, NOT_APPLICABLE
    }

    /** 手持ちアイテムが何らかの付加スキルの要求アイテムか（金床の分岐判定用）。 */
    public static boolean isBonusItem(ItemStack held) {
        return matchBonus(held) != null;
    }

    /** 手持ちアイテムに対応する付加スキルの表示名（メッセージ用）。 */
    public static String bonusDisplayName(ItemStack held) {
        String id = matchBonus(held);
        return id == null ? "" : bonusName(id);
    }

    /** 要求アイテム → 付加スキル id。暗視 = 8分暗視ポーション、オートメイス = ヘビーコア。 */
    private static String matchBonus(ItemStack held) {
        if (held.getType() == Material.POTION
                && held.getItemMeta() instanceof PotionMeta potion
                && potion.getBasePotionType() == PotionType.LONG_NIGHT_VISION) {
            return BONUS_NIGHT_VISION;
        }
        if (held.getType() == Material.HEAVY_CORE) {
            return BONUS_AUTO_MACE;
        }
        if (held.getType() == Material.DRAGON_BREATH) {
            return BONUS_ATTACK_LINGER;
        }
        return null;
    }

    /** 付加スキルを付与する。要求アイテム1個の消費は呼び出し側で行う。 */
    public static BonusResult grantBonus(ItemStack placed, ItemStack held) {
        String id = matchBonus(held);
        if (id == null || !ItemEnhancer.isEnhanceable(placed) || !appliesTo(id, placed)) {
            return BonusResult.NOT_APPLICABLE;
        }
        ItemMeta meta = placed.getItemMeta();
        if (meta == null) {
            return BonusResult.NOT_APPLICABLE;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> list = bonusList(pdc);
        if (list.contains(id)) {
            return BonusResult.ALREADY;
        }
        list.add(id);
        pdc.set(BONUS, PersistentDataType.STRING, String.join(",", list));
        ItemEnhancer.rebuild(placed, meta);
        placed.setItemMeta(meta);
        return BonusResult.GRANTED;
    }

    /** 付加スキルごとの装備条件（暗視 = ヘルメット、オートメイス = メイス、攻撃滞留 = 剣）。 */
    private static boolean appliesTo(String id, ItemStack item) {
        return switch (id) {
            case BONUS_NIGHT_VISION -> item.getType().name().endsWith("_HELMET");
            case BONUS_AUTO_MACE -> item.getType() == Material.MACE;
            case BONUS_ATTACK_LINGER -> item.getType().name().endsWith("_SWORD");
            default -> false;
        };
    }

    private static List<String> bonusList(PersistentDataContainer pdc) {
        String raw = pdc.get(BONUS, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    /** アイテムが指定の付加スキルを持つか。 */
    public static boolean hasBonus(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && bonusList(meta.getPersistentDataContainer()).contains(id);
    }

    /**
     * 付加スキル用の強化段階（0〜3）。レベリングスキルと同じ Lv50/70/100 閾値だが、
     * 有効化ゲート（Lv30）は無く、低レベルでも段階 0 として常時使用できる。
     */
    public static int bonusStage(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        int level = meta.getPersistentDataContainer()
                .getOrDefault(ItemEnhancer.LEVEL, PersistentDataType.INTEGER, 1);
        int s = 0;
        if (level >= 50) s++;
        if (level >= 70) s++;
        if (level >= 100) s++;
        return s;
    }

    // ---- 周期効果 ----

    /**
     * 付加スキルの周期付与（5秒 = 100tick ごとに呼ぶ）。
     * 暗視: ヘルメット装備中、暗視10秒を上書き付与。破壊寸前のヘルメットは効果停止。
     */
    public static void tickBonusEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet == null || !hasBonus(helmet, BONUS_NIGHT_VISION) || ItemEnhancer.isBroken(helmet)) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, 200, 0, true, false, true));
        }
    }
}
