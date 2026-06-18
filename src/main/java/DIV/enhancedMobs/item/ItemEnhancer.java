package DIV.enhancedMobs.item;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.text.Component;
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
 *   <li>レベル: 1レベルにつき固定ステータス加算。レベルギャップ: 30（精錬&gt;=1 必要）、50（鍛造済み必要）、上限 70。</li>
 *   <li>精錬: 1精錬につきアイテム元ステータスの 20% を加算（精錬 5 = +100% = 2倍）。
 *       ネザライトは最大 10 まで可能; 精錬 10 で Lv70 ゲートが解除され上限が 100 になる。</li>
 *   <li>鍛造: メイスでアイテムを鍛造済みマーク（Lv50 ゲートを通過可能にする）。</li>
 *   <li>神格化: 最大段ビーコンでアイテムを神格化し、レベル上限を {@link #DEIFY_BONUS} 解放（100→120 / 70→90）。</li>
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
    public static final NamespacedKey DEIFIED = k("item_deified");
    public static final NamespacedKey CAST = k("item_cast");
    public static final NamespacedKey CAST_EXTENDED = k("item_cast_extended");
    public static final NamespacedKey BROKEN = k("item_broken");

    /** 神格化で解放されるレベル上限の増分。 */
    public static final int DEIFY_BONUS = 20;
    /** 鋳造系（弓・盾）が Lv50 ゲートを通過するのに要する鋳造回数。 */
    public static final int CAST_GATE_50 = 8;
    /** 鋳造系（弓・盾）が Lv70→100 ゲートを通過するのに要する鋳造回数（残響の欠片で 8→16 解放）。 */
    public static final int CAST_GATE_70 = 16;

    /** 破壊寸前状態の解除に必要な耐久割合。 */
    public static final double BROKEN_CLEAR_RATIO = 0.75;
    private static final NamespacedKey M_ATK = k("ench_atk");
    private static final NamespacedKey M_SPD = k("ench_spd");
    private static final NamespacedKey M_ARM = k("ench_arm");
    private static final NamespacedKey M_TUF = k("ench_tuf");
    private static final NamespacedKey M_KB = k("ench_kb");
    private static final NamespacedKey M_HP = k("skill_hp");
    private static final NamespacedKey M_MINE = k("ench_mine");
    /**
     * 採掘系のレベルあたり採掘効率（mining_efficiency 属性への加算）。即時化は 採掘速度 ≥ 硬度×30
     * （深層岩=90 / 深層岩鉱石=135）。採掘効率5(+26)＋採掘速度上昇II の想定。
     * Lv100 で深層岩鉱石(135)を確実に超えるよう、採掘速度上昇がボーナスに乗らない最悪ケースでも
     * 足りる係数にしている（空論値。バランスなどを鑑みて修正の可能性あり）。
     */
    private static final double MINING_PER_LEVEL = 0.9;

    public enum Category {
        WEAPON, ARMOR, TOOL, SHIELD, BOW
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
        if (n.equals("SHIELD")) {
            return Category.SHIELD;
        }
        if (n.equals("BOW")) {
            return Category.BOW; // 弓は鋳造系レベリング（発射でXP・正鵠を射る抽選）
        }
        // 道具はピッケル・シャベルがフル強化対象（採掘系）。他の耐久品は isRepairOnly（耐久回復のみ）。
        if (n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL")) {
            return Category.TOOL;
        }
        return null;
    }

    /** 採掘系（採掘速度ボーナス対象）: ピッケル・シャベル・斧。斧は武器カテゴリだが採掘もこなす。 */
    private static boolean isMiningTool(ItemStack item) {
        String n = item.getType().name();
        return n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_AXE");
    }

    /**
     * 修理のみ対応か: 強化カテゴリ外だが、修理素材が定義された耐久品
     * （クワ・クロスボウ・ハサミ・釣竿等）。金床に載せて耐久回復だけ行える。
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

    /** 当プラグインの強化データ（レベル）を実際に保持しているか。 */
    public static boolean isEnhanced(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(LEVEL, PersistentDataType.INTEGER);
    }

    /**
     * 現在のマテリアル基準でステータス・耐久・ロアを PDC から再構築する。
     * 鍛冶台でのマテリアル変更（ダイヤ→ネザライト）後などに呼び、上限・基礎値を正しく追従させる。
     */
    public static void refresh(ItemStack item) {
        if (!isEnhanceable(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        rebuild(item, meta);
        item.setItemMeta(meta);
    }

    private static boolean isNetherite(ItemStack item) {
        return item.getType().name().startsWith("NETHERITE");
    }

    public static int maxRefine(ItemStack item) {
        // 盾は元ステータスが無いぶん精錬を多く要求（精錬25で Lv70 ゲート解除＝Lv100到達）。
        if (item.getType() == Material.SHIELD) {
            return 25;
        }
        return isNetherite(item) ? 10 : 5;
    }

    public static int cap(ItemStack item) {
        // メイス・盾・弓はネザライト同様 Lv100 まで（Lv70 ゲートはメイス精錬5 / 盾精錬25 / 弓精錬5で解除）
        int base = isNetherite(item) || item.getType() == Material.MACE
                || item.getType() == Material.SHIELD || item.getType() == Material.BOW
                ? 100 : 70;
        return isDeified(item) ? base + DEIFY_BONUS : base; // 神格化で上限を +DEIFY_BONUS 解放
    }

    /** 鋳造系（弓・盾）か。Lv50 ゲートを鍛造ではなく鋳造で通過する。 */
    public static boolean usesCasting(ItemStack item) {
        Category c = category(item);
        return c == Category.BOW || c == Category.SHIELD;
    }

    /** 現在の鋳造回数。 */
    public static int castCount(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta == null ? 0 : meta.getPersistentDataContainer().getOrDefault(CAST, PersistentDataType.INTEGER, 0);
    }

    /** 残響の欠片で 8→16 解放済みか。 */
    public static boolean isCastExtended(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(CAST_EXTENDED, PersistentDataType.BYTE);
    }

    /** 現在の鋳造上限（残響の欠片解放前は {@link #CAST_GATE_50}、解放後は {@link #CAST_GATE_70}）。 */
    public static int castCap(ItemStack item) {
        return isCastExtended(item) ? CAST_GATE_70 : CAST_GATE_50;
    }

    /**
     * 鋳造 +1（鋳造系のみ。現在の上限 {@link #castCap} まで）。
     * 8 で Lv50 ゲート、16 で Lv70→100 ゲートが解放される。上限到達済みなら false。
     */
    public static boolean cast(ItemStack item) {
        if (!isEnhanceable(item) || !usesCasting(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var pdc = meta.getPersistentDataContainer();
        int c = pdc.getOrDefault(CAST, PersistentDataType.INTEGER, 0);
        int cap = pdc.has(CAST_EXTENDED, PersistentDataType.BYTE) ? CAST_GATE_70 : CAST_GATE_50;
        if (c >= cap) {
            return false;
        }
        pdc.set(CAST, PersistentDataType.INTEGER, c + 1);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    /** 残響の欠片で鋳造上限を 8→16 へ解放する結果。 */
    public enum CastExtendResult { EXTENDED, NEED_CASTS, ALREADY, NOT_APPLICABLE }

    /**
     * 残響の欠片で鋳造上限を {@link #CAST_GATE_50}→{@link #CAST_GATE_70} へ解放する。
     * 鋳造が 8 回に達している鋳造系アイテムにのみ有効。
     */
    public static CastExtendResult extendCast(ItemStack item) {
        if (!isEnhanceable(item) || !usesCasting(item)) {
            return CastExtendResult.NOT_APPLICABLE;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return CastExtendResult.NOT_APPLICABLE;
        }
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(CAST_EXTENDED, PersistentDataType.BYTE)) {
            return CastExtendResult.ALREADY;
        }
        if (pdc.getOrDefault(CAST, PersistentDataType.INTEGER, 0) < CAST_GATE_50) {
            return CastExtendResult.NEED_CASTS;
        }
        pdc.set(CAST_EXTENDED, PersistentDataType.BYTE, (byte) 1);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return CastExtendResult.EXTENDED;
    }

    /** 神格化済みか。 */
    public static boolean isDeified(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && isDeified(meta);
    }

    private static boolean isDeified(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(DEIFIED, PersistentDataType.BYTE);
    }

    /** 神格化の結果。 */
    public enum DeifyResult { DEIFIED, ALREADY, NOT_APPLICABLE }

    /**
     * アイテムを神格化し、レベル上限を {@link #DEIFY_BONUS} 解放する（100→120 / 70→90）。
     * 既に神格化済みなら {@link DeifyResult#ALREADY}、対象外なら {@link DeifyResult#NOT_APPLICABLE}。
     */
    public static DeifyResult deify(ItemStack item) {
        if (!isEnhanceable(item)) {
            return DeifyResult.NOT_APPLICABLE;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return DeifyResult.NOT_APPLICABLE;
        }
        if (isDeified(meta)) {
            return DeifyResult.ALREADY;
        }
        meta.getPersistentDataContainer().set(DEIFIED, PersistentDataType.BYTE, (byte) 1);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return DeifyResult.DEIFIED;
    }

    /** 素材別 XP 要求に掛かる基準係数。Lv120 までを終端 EndContent 相当の長丁場にするための土台。 */
    /** アイテムの現在レベルでの必要XP（ロア表示などの外部用）。 */
    static int requiredXp(ItemStack item) {
        int level = 1;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            level = meta.getPersistentDataContainer().getOrDefault(LEVEL, PersistentDataType.INTEGER, 1);
        }
        return requiredXp(item, level);
    }

    /** 指定レベルでの必要XP。素材ごとの基礎値にレベル倍率カーブを掛ける（旧: 一律 ×4 を廃止）。 */
    static int requiredXp(ItemStack item, int level) {
        return Math.max(1, (int) Math.round(baseRequiredXp(item) * levelXpMultiplier(level) * xpMultiplier()));
    }

    /**
     * レベルに応じた必要XPの倍率カーブ。素材基礎値に対し、
     * L1=×1.0 → L30=×1.5 → L70=×3.0 → L100=×9.0 → L120=×18.0 を線形補間で緩やかに上昇させる。
     * （例: ネザライト基礎80 → 80 / 120 / 240 / 720 / 1440）
     */
    private static double levelXpMultiplier(int level) {
        if (level <= 1) {
            return 1.0;
        }
        if (level <= 30) {
            return lerp(1, 30, 1.0, 1.5, level);
        }
        if (level <= 70) {
            return lerp(30, 70, 1.5, 3.0, level);
        }
        if (level <= 100) {
            return lerp(70, 100, 3.0, 9.0, level);
        }
        if (level <= 120) {
            return lerp(100, 120, 9.0, 18.0, level);
        }
        return 18.0;
    }

    private static double lerp(int x0, int x1, double y0, double y1, int x) {
        return y0 + (y1 - y0) * (double) (x - x0) / (x1 - x0);
    }

    /** L1 から指定レベルへ到達するまでの累計必要XP（その素材のカーブ）。 */
    static int cumulativeXp(ItemStack item, int level) {
        int total = 0;
        for (int l = 1; l < level; l++) {
            total += requiredXp(item, l);
        }
        return total;
    }

    /** これまで投入された総経験値（到達分の累計 + 現在の途中XP）。素材変更時に保存する量。 */
    public static int totalInvestedXp(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        var pdc = meta.getPersistentDataContainer();
        int level = pdc.getOrDefault(LEVEL, PersistentDataType.INTEGER, 1);
        int partial = pdc.getOrDefault(XP, PersistentDataType.INTEGER, 0);
        return cumulativeXp(item, level) + partial;
    }

    /**
     * 総経験値から、このアイテム（現在の素材）のレベルと途中XPを再導出して PDC に設定する。
     * ダイヤ→ネザライトのように必要XPカーブが重くなる素材では、同じ総経験値でもレベルが下がる
     * （＝レベルではなく投入経験値を維持する）。レベルは現素材の上限で頭打ち。
     */
    public static void setLevelFromTotalXp(ItemStack item, int totalXp) {
        if (!isEnhanceable(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int cap = cap(item);
        int level = 1;
        int remaining = Math.max(0, totalXp);
        while (level < cap) {
            int req = requiredXp(item, level);
            if (remaining < req) {
                break;
            }
            remaining -= req;
            level++;
        }
        if (level >= cap) {
            remaining = Math.min(remaining, requiredXp(item, level)); // 上限到達はバー満タン止め
        }
        var pdc = meta.getPersistentDataContainer();
        pdc.set(LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(XP, PersistentDataType.INTEGER, remaining);
        item.setItemMeta(meta);
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
        int req = requiredXp(item, level);
        boolean leveled = false;
        while (xp >= req && canPass(item, level, refine, forged)) {
            xp -= req;
            level++;
            leveled = true;
            if (level >= cap(item)) {
                break;
            }
            req = requiredXp(item, level); // 次レベルの要求値で継続（カーブ上昇に追従）
        }
        req = requiredXp(item, level); // 最終レベルの要求値（ロアバー用）
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
        if (level == GATE_REFINE && refine < 1) return false;
        if (usesCasting(item)) {
            // 弓・盾は鋳造で通過：Lv50=鋳造8（グロウストーン）、Lv70→100=鋳造16（残響の欠片で 8→16 解放）。
            if (level == GATE_FORGE && castCount(item) < CAST_GATE_50) return false;
            if (level == 70 && castCount(item) < CAST_GATE_70) return false;
        } else {
            if (level == GATE_FORGE && forged < GATE_FORGE) return false; // 鍛造（メイス刻印）
            if (level == 70 && refine < maxRefine(item)) return false;    // 精錬MAX（ネザライト10 / メイス5）
        }
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

    /**
     * 即座にレベル上限・精錬上限へ引き上げる（ドラゴンの頭での即時マックス用）。
     * 鍛造マークも付与し、XP は 0、未抽選ならスキルも抽選する。
     * 神格化ぶん（{@link #DEIFY_BONUS}）は含めず、基礎上限（100 / 70）まで。神格化後の 100→120 は自力で。
     */
    public static boolean maxOut(ItemStack item) {
        if (!isEnhanceable(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var pdc = meta.getPersistentDataContainer();
        int max = cap(item);
        if (isDeified(item)) {
            max -= DEIFY_BONUS; // ドラゴンヘッドは神格化ぶんを与えない（基礎上限止まり）
        }
        pdc.set(LEVEL, PersistentDataType.INTEGER, max);
        pdc.set(XP, PersistentDataType.INTEGER, 0);
        pdc.set(REFINE, PersistentDataType.INTEGER, maxRefine(item));
        pdc.set(FORGED, PersistentDataType.INTEGER, GATE_FORGE);
        pdc.set(CAST, PersistentDataType.INTEGER, CAST_GATE_70);
        pdc.set(CAST_EXTENDED, PersistentDataType.BYTE, (byte) 1);
        ItemSkills.rollIfNeeded(item, pdc, max);
        rebuild(item, meta);
        item.setItemMeta(meta);
        return true;
    }

    /** 破壊寸前状態か。 */
    public static boolean isBroken(ItemStack item) {
        if (item == null) {
            return false; // 防具スロット未装備でも安全に false
        }
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
        // 盾は両手のどちらで持っても効くよう HAND グループ（メインハンド＋オフハンド）。
        EquipmentSlotGroup group = cat == Category.ARMOR ? armorGroup(item)
                : cat == Category.SHIELD ? EquipmentSlotGroup.HAND
                : EquipmentSlotGroup.MAINHAND;

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
        } else if (cat == Category.SHIELD) {
            // 盾は元ステータスが無いため、精錬はレベル上昇分を 4%/精錬で増幅する（防具より低めの係数）。
            double refMult = 1.0 + 0.04 * refine;
            // ネザライトコーティング（付加スキル）: ネザライトレギンス相当の素性能を上乗せする。
            boolean coated = ItemSkills.hasBonus(meta, ItemSkills.BONUS_NETHERITE_COATING);
            double coatArmor = coated ? 6.0 : 0.0;
            double coatTough = coated ? 3.0 : 0.0;
            double coatKb = coated ? 0.1 : 0.0;
            addBonus(meta, Attribute.ARMOR, M_ARM, level * 0.15 * refMult + coatArmor, group);
            addBonus(meta, Attribute.KNOCKBACK_RESISTANCE, M_KB, level * 0.01 * refMult + coatKb, group);
            addBonus(meta, Attribute.ARMOR_TOUGHNESS, M_TUF, coatTough, group);
        } else if (cat == Category.ARMOR) {
            // 防具性能上昇スキル: 素の防具性能の 20/40/60/120% を加算（無効時 0）
            double boost = ItemSkills.armorBoostPct(meta, level);
            addBonus(meta, Attribute.ARMOR, M_ARM,
                    level * 0.25 + (r + boost) * baseStat(defaults, Attribute.ARMOR, 0.0), group);
            addBonus(meta, Attribute.ARMOR_TOUGHNESS, M_TUF,
                    level * 0.1 + (r + boost) * baseStat(defaults, Attribute.ARMOR_TOUGHNESS, 0.0), group);
        }
        // 採掘系（ピッケル・シャベル・斧）はレベルに応じて採掘速度が少し上がる。
        // さらに高速採掘スキルがあれば段階ぶん上乗せ（無効時 0）。
        if (isMiningTool(item)) {
            addBonus(meta, Attribute.MINING_EFFICIENCY, M_MINE,
                    level * MINING_PER_LEVEL + ItemSkills.fastMiningBonus(meta, level), group);
        }
        // 最大体力増加スキル（防具のみ抽選されるが、判定はスキル有無で行う）
        addBonus(meta, Attribute.MAX_HEALTH, M_HP, ItemSkills.healthBonus(meta, level), group);
        applyDurability(item, meta, cat, level, refine);
        ItemSkills.applyUnbreaking(meta, level);
        ItemSkills.applyFortune(meta, level);
    }

    /**
     * 耐久上限 = (素 + レベル/精錬補正) × 耐久強化スキル倍率。
     * 武器はレベル/精錬の耐久補正なし（スキル倍率のみ）。盾はレベル上昇分を精錬4%/精錬で増幅。
     */
    private static void applyDurability(ItemStack item, ItemMeta meta, Category cat, int level, int refine) {
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int base = item.getType().getMaxDurability();
        if (base <= 0) {
            return;
        }
        double r = 0.2 * refine;
        double withLevel;
        if (cat == Category.WEAPON) {
            withLevel = base;
        } else if (cat == Category.SHIELD) {
            // ネザライトコーティング時は素の耐久をネザライトレギンス(555)まで底上げ。
            int shieldBase = ItemSkills.hasBonus(meta, ItemSkills.BONUS_NETHERITE_COATING)
                    ? Math.max(base, 555) : base;
            withLevel = shieldBase + level * 3 * (1.0 + 0.04 * refine); // 防具(+5/Lv)より低め、精錬で増幅
        } else {
            withLevel = base + level * 5 + r * base;
        }
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
        lore.add(Lang.render("emob.item.lore.level", Component.text(level), Component.text(progress)));
        if (refine > 0) {
            lore.add(Lang.render("emob.item.lore.refine",
                    Component.text(refine), Component.text(maxRefine(item))));
        }
        if (usesCasting(item)) {
            int castCap = meta.getPersistentDataContainer().has(CAST_EXTENDED, PersistentDataType.BYTE)
                    ? CAST_GATE_70 : CAST_GATE_50;
            int cast = Math.min(meta.getPersistentDataContainer().getOrDefault(CAST, PersistentDataType.INTEGER, 0),
                    castCap);
            lore.add(Lang.render("emob.item.lore.cast", Component.text(cast), Component.text(castCap)));
        } else if (forged >= GATE_FORGE) {
            lore.add(Lang.render("emob.item.lore.forged"));
        }
        if (isDeified(meta)) {
            lore.add(Lang.render("emob.item.lore.deified"));
        }
        lore.addAll(ItemSkills.loreLines(meta, level));
        if (meta.getPersistentDataContainer().has(BROKEN, PersistentDataType.BYTE)) {
            lore.add(Lang.render("emob.item.lore.broken"));
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
