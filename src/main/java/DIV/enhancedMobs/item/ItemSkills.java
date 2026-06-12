package DIV.enhancedMobs.item;

import DIV.enhancedMobs.EnhancedMobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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
    public static final String SKILL_ENERGY_ABSORB = "energy_absorb";
    public static final String SKILL_DEBUFF_IMMUNITY = "debuff_immunity";
    public static final String SKILL_ARMOR_BOOST = "armor_boost";
    public static final String SKILL_LION_HEART = "lion_heart";
    public static final String SKILL_SET_BONUS = "set_bonus";
    public static final String SKILL_PAST_GIFT = "past_gift";
    public static final String SKILL_HERO_HYMN = "hero_hymn";
    public static final String SKILL_ASAHI = "asahi_mourning";
    private static final String BONUS_NIGHT_VISION = "night_vision";
    public static final String BONUS_AUTO_MACE = "auto_mace";
    public static final String BONUS_ATTACK_LINGER = "attack_linger";
    public static final String BONUS_BULK_BREAK = "bulk_break";

    /** 設定可能なスキル id 一覧（コマンドのバリデーション・補完用）。 */
    public static final List<String> SKILL_IDS = List.of(
            SKILL_DURABILITY, SKILL_FLYING, SKILL_HEALTH, SKILL_CHARGE, SKILL_VALOR, SKILL_DASH,
            SKILL_ALL_IN, SKILL_JUST_BLOCK, SKILL_TAKENOKO, SKILL_ENERGY_ABSORB,
            SKILL_DEBUFF_IMMUNITY, SKILL_ARMOR_BOOST, SKILL_LION_HEART, SKILL_SET_BONUS,
            SKILL_PAST_GIFT, SKILL_HERO_HYMN, SKILL_ASAHI);

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
    /** エネルギー吸収: 段階ごとのクールタイム（秒）。 */
    public static final int[] ENERGY_COOLDOWN_SEC = {13, 12, 10, 5};
    /** エネルギー吸収: 段階ごとの回復量（HP。3/4/5/10 ハート）。 */
    public static final double[] ENERGY_HEAL = {6, 8, 10, 20};
    /** デバフ免疫: 段階ごとのクールタイム（秒）。 */
    public static final int[] DEBUFF_COOLDOWN_SEC = {30, 28, 26, 15};
    /** 防具性能上昇: 段階ごとの素防具性能（ARMOR/ARMOR_TOUGHNESS）に対する加算割合。 */
    static final double[] ARMOR_BOOST_PCT = {0.2, 0.4, 0.6, 1.2};
    /** 獅子の心臓: 段階ごとのクールタイム（秒）。 */
    public static final int[] LION_COOLDOWN_SEC = {150, 140, 130, 100};
    /** セット商法: 最大体力加算。 */
    private static final double SET_BONUS_HP_AMOUNT = 10;
    /** セット商法: 攻撃力加算。 */
    private static final double SET_BONUS_ATK_AMOUNT = 5;
    /** 過去からの贈り物: 死亡時に未来へ送る防御力の割合。 */
    public static final double PAST_GIFT_RATIO = 0.25;
    /** 朽ちた英雄の賛歌: 雷の追加ダメージ（攻撃力比）。仕様 50/70/150% を補間（段階2=100%）。 */
    public static final double[] HYMN_DAMAGE_PCT = {0.5, 0.7, 1.0, 1.5};
    /** 朽ちた英雄の賛歌: 回復封印の持続（tick）。 */
    public static final int HYMN_CURSE_TICKS = 10;
    /** 旭の弔い: 燃焼中の対象が受ける炎ダメージ増加。仕様 20/30/70% を補間（段階2=50%）。 */
    public static final double[] ASAHI_FIRE_VULN = {0.20, 0.30, 0.50, 0.70};
    /** 旭の弔い: 攻撃時の着火時間（tick、火属性化）。 */
    public static final int ASAHI_IGNITE_TICKS = 80;
    /** 一括破壊: 段階ごとの連鎖範囲（起点からのチェビシェフ距離）。 */
    public static final int[] BULK_RANGE = {1, 1, 2, 2};
    /** 一括破壊: 1回の発動で破壊できるブロック総数（起点含む）。 */
    public static final int BULK_MAX_BLOCKS = 48;

    /** セット商法: プレイヤーへ付ける transient モディファイアのキー。 */
    private static final NamespacedKey SET_BONUS_HP_KEY = k("set_bonus_hp");
    private static final NamespacedKey SET_BONUS_ATK_KEY = k("set_bonus_atk");

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
            pool.add(SKILL_ASAHI);
        }
        if (n.endsWith("_SWORD")) {
            pool.add(SKILL_VALOR); // 剣限定
            pool.add(SKILL_DASH);
            pool.add(SKILL_ENERGY_ABSORB);
            pool.add(SKILL_HERO_HYMN);
        }
        if (n.endsWith("SPEAR")) {
            pool.add(SKILL_CHARGE); // 槍限定
        }
        if (ItemEnhancer.category(item) == ItemEnhancer.Category.ARMOR) {
            pool.add(SKILL_HEALTH); // 防具限定
            pool.add(SKILL_JUST_BLOCK);
            pool.add(SKILL_DEBUFF_IMMUNITY);
            pool.add(SKILL_ARMOR_BOOST);
            pool.add(SKILL_PAST_GIFT);
        }
        if (n.endsWith("_CHESTPLATE")) {
            pool.add(SKILL_LION_HEART); // チェストプレート限定
            pool.add(SKILL_SET_BONUS);
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

    /** スキル・付加スキルの lore 行（updateLore から呼ばれる）。説明文は常時表示。 */
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
            // 未有効化（Lv30未満）は初期段階の数値で説明する
            addDescription(lines, skillDescription(id, Math.max(s, 0)));
        }
        int bonusS = bonusStageOf(level);
        for (String bonus : bonusList(pdc)) {
            lines.add(Component.text("付加: " + bonusName(bonus), NamedTextColor.LIGHT_PURPLE));
            addDescription(lines, bonusDescription(bonus, bonusS));
        }
        return lines;
    }

    private static void addDescription(List<Component> lines, List<String> description) {
        for (String line : description) {
            lines.add(Component.text("  " + line, NamedTextColor.GRAY));
        }
    }

    /** レベリングスキルの説明文（現在の強化段階の数値入り）。 */
    private static List<String> skillDescription(String id, int s) {
        return switch (id) {
            case SKILL_DURABILITY -> List.of(
                    String.format("耐久値の上限が ×%.1f になる", DURA_MULT[s]),
                    "耐久エンチャントが付いていれば +" + UNBR_BONUS[s]);
            case SKILL_FLYING -> List.of(
                    "右クリックで斧を投げる（敵に当たるか着弾で戻る）",
                    String.format("射程 %.0fブロック / CT %d秒", FLYING_RANGE[s], FLYING_COOLDOWN_SEC[s]));
            case SKILL_HEALTH -> List.of(
                    String.format("装備中、最大体力 +%.0f", HEALTH_BONUS[s]));
            case SKILL_CHARGE -> List.of(
                    "突進すると満腹度 +" + CHARGE_HUNGER[s] + " 回復し、",
                    "5秒間落下ダメージを受けない");
            case SKILL_VALOR -> List.of(
                    "攻撃するたび攻撃力上昇の効果を獲得・強化していく",
                    String.format("上限レベル %d / 効果時間 %d秒", VALOR_CAP[s], VALOR_DURATION_SEC[s]));
            case SKILL_DASH -> s >= 2
                    ? List.of(
                            String.format("右クリックで前方へ突進する（CT %.1f秒）", DASH_COOLDOWN_TICKS[s] / 20.0),
                            "突進の道中、敵を攻撃力ぶん切りつける")
                    : List.of(
                            String.format("右クリックで前方へ突進する（CT %.1f秒）", DASH_COOLDOWN_TICKS[s] / 20.0));
            case SKILL_ALL_IN -> List.of(
                    String.format("攻撃速度が %.1f に固定される代わりに", ALL_IN_SPEED[s]),
                    String.format("攻撃力 +%.0f", ALL_IN_ATK[s]));
            case SKILL_JUST_BLOCK -> List.of(
                    String.format("被弾時 %.0f%% で攻撃を完全にブロックする", JUST_BLOCK_CHANCE[s] * 100));
            case SKILL_TAKENOKO -> List.of(
                    String.format("攻撃した敵を %.0fブロック打ち上げ、", TAKENOKO_HEIGHT[s]),
                    "足元にたけのこを生やす");
            case SKILL_ENERGY_ABSORB -> List.of(
                    "接地して静止中に右クリックで",
                    String.format("HPを %.0fハート回復（CT %d秒）", ENERGY_HEAL[s] / 2, ENERGY_COOLDOWN_SEC[s]));
            case SKILL_DEBUFF_IMMUNITY -> List.of(
                    String.format("デバフの付与を1つ自動で防ぐ（CT %d秒）", DEBUFF_COOLDOWN_SEC[s]));
            case SKILL_ARMOR_BOOST -> List.of(
                    String.format("素の防具性能の %.0f%% を追加で獲得する", ARMOR_BOOST_PCT[s] * 100));
            case SKILL_LION_HEART -> List.of(
                    "HPが10%以下になる攻撃を半ハートで耐え、",
                    String.format("トーテム効果と5秒間の耐性Vを得る（CT %d秒）", LION_COOLDOWN_SEC[s]));
            case SKILL_SET_BONUS -> List.of(
                    "防具4部位の素材が共通なら",
                    String.format("再生I・最大体力 +%.0f・攻撃力 +%.0f", SET_BONUS_HP_AMOUNT, SET_BONUS_ATK_AMOUNT));
            case SKILL_PAST_GIFT -> List.of(
                    String.format("死亡時、防御力の%.0f%%を未来へ送る", PAST_GIFT_RATIO * 100),
                    "次のリスポーンで防御力上昇として受け取る",
                    "（受領中に死亡すると効果を失い、送らない）");
            case SKILL_HERO_HYMN -> List.of(
                    String.format("攻撃時、対象の回復を%.1f秒間封じ", HYMN_CURSE_TICKS / 20.0),
                    String.format("雷とともに攻撃力の%.0f%%を追加で与える", HYMN_DAMAGE_PCT[s] * 100),
                    "（回復封印中は不死の蘇生も失敗する）");
            case SKILL_ASAHI -> List.of(
                    String.format("攻撃が火属性になる（%.0f秒着火）", ASAHI_IGNITE_TICKS / 20.0),
                    String.format("燃えている対象は受ける炎ダメージ +%.0f%%、", ASAHI_FIRE_VULN[s] * 100),
                    "回復効果を受けられない（不死の蘇生も失敗）");
            default -> List.of();
        };
    }

    /** 付加スキルの説明文（現在の段階の数値入り）。 */
    private static List<String> bonusDescription(String id, int s) {
        return switch (id) {
            case BONUS_NIGHT_VISION -> List.of("装備中、暗視効果を得る");
            case BONUS_AUTO_MACE -> List.of(
                    "スニーク右クリックで狙った敵をメイスが自動攻撃",
                    String.format("上昇 %.0fブロック / CT %d秒", AUTO_MACE_RISE[s], AUTO_MACE_COOLDOWN_SEC[s]));
            case BONUS_ATTACK_LINGER -> List.of(
                    String.format("攻撃時 %.0f%% でダメージを記憶した", LINGER_CHANCE[s] * 100),
                    "滞留クラウドが発生する（1秒ごと×3回）");
            case BONUS_BULK_BREAK -> List.of(
                    "鉱石などの同系統ブロックをまとめて採掘",
                    String.format("範囲 %d / 最大 %d個", BULK_RANGE[s], BULK_MAX_BLOCKS));
            default -> List.of();
        };
    }

    /** スキルの表示名（サイドバー等の外部表示用）。 */
    public static String skillDisplayName(String id) {
        return skillName(id);
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
            case SKILL_ENERGY_ABSORB -> "エネルギー吸収";
            case SKILL_DEBUFF_IMMUNITY -> "デバフ免疫";
            case SKILL_ARMOR_BOOST -> "防具性能上昇";
            case SKILL_LION_HEART -> "獅子の心臓";
            case SKILL_SET_BONUS -> "セット商法";
            case SKILL_PAST_GIFT -> "過去からの贈り物";
            case SKILL_HERO_HYMN -> "朽ちた英雄の賛歌";
            case SKILL_ASAHI -> "旭の弔い";
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

    /** 防具性能上昇スキルによる素防具性能への加算割合（無効時は 0）。ItemEnhancer.applyStats から呼ばれる。 */
    static double armorBoostPct(ItemMeta meta, int level) {
        int s = activeStageOf(meta, level, SKILL_ARMOR_BOOST);
        return s < 0 ? 0 : ARMOR_BOOST_PCT[s];
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
            case BONUS_BULK_BREAK -> "一括破壊";
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
        if (BULK_BREAK_ITEMS.contains(held.getType())) {
            return BONUS_BULK_BREAK;
        }
        return null;
    }

    /** 一括破壊の要求アイテム候補（ティア一致の検証は grantBonus で行う）。 */
    private static final List<Material> BULK_BREAK_ITEMS = List.of(
            Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
            Material.NETHERITE_BLOCK, Material.COPPER_BLOCK);

    /**
     * 一括破壊: ピッケルのティアに対応する要求ブロック。
     * 木・石ピッケルおよびピッケル以外は対象外（null）。
     */
    private static Material bulkBlockFor(Material pickaxe) {
        return switch (pickaxe.name()) {
            case "IRON_PICKAXE" -> Material.IRON_BLOCK;
            case "GOLDEN_PICKAXE" -> Material.GOLD_BLOCK;
            case "DIAMOND_PICKAXE" -> Material.DIAMOND_BLOCK;
            case "NETHERITE_PICKAXE" -> Material.NETHERITE_BLOCK;
            case "COPPER_PICKAXE" -> Material.COPPER_BLOCK;
            default -> null;
        };
    }

    /** 一括破壊: 設置アイテムがピッケルで、要求ブロックがそのティアに一致するか。 */
    private static boolean bulkTierMatches(ItemStack placed, ItemStack held) {
        Material required = bulkBlockFor(placed.getType());
        return required != null && required == held.getType();
    }

    /** 付加スキルを付与する。要求アイテム1個の消費は呼び出し側で行う。 */
    public static BonusResult grantBonus(ItemStack placed, ItemStack held) {
        String id = matchBonus(held);
        if (id == null || !ItemEnhancer.isEnhanceable(placed) || !appliesTo(id, placed)) {
            return BonusResult.NOT_APPLICABLE;
        }
        if (BONUS_BULK_BREAK.equals(id) && !bulkTierMatches(placed, held)) {
            return BonusResult.NOT_APPLICABLE; // ティア不一致（木・石ピッケル含む）
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

    /** 付加スキルごとの装備条件（暗視 = ヘルメット、オートメイス = メイス、攻撃滞留 = 剣、一括破壊 = ピッケル）。 */
    private static boolean appliesTo(String id, ItemStack item) {
        return switch (id) {
            case BONUS_NIGHT_VISION -> item.getType().name().endsWith("_HELMET");
            case BONUS_AUTO_MACE -> item.getType() == Material.MACE;
            case BONUS_ATTACK_LINGER -> item.getType().name().endsWith("_SWORD");
            case BONUS_BULK_BREAK -> item.getType().name().endsWith("_PICKAXE");
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
        return bonusStageOf(level);
    }

    private static int bonusStageOf(int level) {
        int s = 0;
        if (level >= 50) s++;
        if (level >= 70) s++;
        if (level >= 100) s++;
        return s;
    }

    // ---- 周期効果 ----

    /**
     * 付加スキル・常時スキルの周期付与（5秒 = 100tick ごとに呼ぶ）。
     * <ul>
     *   <li>暗視: ヘルメット装備中、暗視10秒を上書き付与。破壊寸前のヘルメットは効果停止。</li>
     *   <li>セット商法: 条件成立中、再生I 130t を更新しつつ transient モディファイアを存在保証。</li>
     * </ul>
     */
    public static void tickBonusEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null && hasBonus(helmet, BONUS_NIGHT_VISION) && !ItemEnhancer.isBroken(helmet)) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 200, 0, true, false, true));
            }
            tickSetBonus(player);
        }
    }

    /**
     * セット商法: 防具4部位が同素材（Material 名の最初の "_" までが共通）で揃っていれば、
     * 再生I + 最大体力+10 + 攻撃力+5。モディファイアは冪等に存在保証/除去する。
     */
    private static void tickSetBonus(Player player) {
        boolean active = setBonusActive(player);
        ensureTransientModifier(player, Attribute.MAX_HEALTH, SET_BONUS_HP_KEY, SET_BONUS_HP_AMOUNT, active);
        ensureTransientModifier(player, Attribute.ATTACK_DAMAGE, SET_BONUS_ATK_KEY, SET_BONUS_ATK_AMOUNT, active);
        if (active) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, 130, 0, true, false, true));
        }
    }

    /** セット商法の成立判定。チェストにスキル有効（破壊寸前除外）+ 4部位の素材プレフィックス共通。 */
    private static boolean setBonusActive(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || activeStage(chest, SKILL_SET_BONUS) < 0 || ItemEnhancer.isBroken(chest)) {
            return false;
        }
        String prefix = materialPrefix(chest.getType());
        if (prefix == null) {
            return false;
        }
        ItemStack[] others = {player.getInventory().getHelmet(),
                player.getInventory().getLeggings(), player.getInventory().getBoots()};
        for (ItemStack piece : others) {
            if (piece == null || !prefix.equals(materialPrefix(piece.getType()))) {
                return false;
            }
        }
        return true;
    }

    /** Material 名の最初の "_" までのプレフィックス。"_" が無い場合（ELYTRA 等）は null = 不一致扱い。 */
    private static String materialPrefix(Material material) {
        String n = material.name();
        int i = n.indexOf('_');
        return i < 0 ? null : n.substring(0, i);
    }

    /**
     * プレイヤー属性への transient モディファイアの冪等な存在保証/除去。
     * 既に同キーのモディファイアがあれば付け直さない（重複追加例外の回避）。
     */
    private static void ensureTransientModifier(Player player, Attribute attribute, NamespacedKey key,
                                                double amount, boolean active) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier m : instance.getModifiers()) {
            if (key.equals(m.getKey())) {
                existing = m;
                break;
            }
        }
        if (active && existing == null) {
            instance.addTransientModifier(new AttributeModifier(
                    key, amount, AttributeModifier.Operation.ADD_NUMBER));
        } else if (!active && existing != null) {
            instance.removeModifier(existing);
        }
    }
}
