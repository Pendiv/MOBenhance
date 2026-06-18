package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * プレイヤー固有の永続状態（PDC）。L2Hostility の {@code PlayerDifficulty} に相当。
 * 現状はネザー・エンド訪問フラグと難易度オフセットを保持。
 * フラグによるプレイヤーの個人に対する加算を、計算式から算出する。
 */
public final class PlayerData {

    public static final NamespacedKey VISITED_NETHER = key("visited_nether");
    public static final NamespacedKey VISITED_END = key("visited_end");
    public static final NamespacedKey DIFFICULTY_OFFSET = key("difficulty_offset");

    // ---- 事故救済（連続死亡で危険度が下がる） ----
    private static final NamespacedKey RELIEF_PERM = key("relief_perm_pct");
    private static final NamespacedKey RELIEF_DEATHS = key("relief_deaths_today");
    private static final NamespacedKey RELIEF_DAY = key("relief_death_day");
    private static final NamespacedKey RELIEF_TEMP_PCT = key("relief_temp_pct");
    private static final NamespacedKey RELIEF_TEMP_FLAT = key("relief_temp_flat");
    private static final NamespacedKey RELIEF_TEMP_EXPIRY = key("relief_temp_expiry_day");

    /** 救済発動に必要な「同一MC日内の死亡回数」しきい値。 */
    private static final int RELIEF_THRESHOLD = 3;
    /** 死亡1回あたりの永続危険度軽減（全体の0.2%）。 */
    private static final double RELIEF_PERM_STEP = 0.002;
    /** 死亡1回あたりの臨時危険度軽減（割合2%）。 */
    private static final double RELIEF_TEMP_PCT_STEP = 0.02;
    /** 死亡1回あたりの臨時危険度軽減（実数8）。 */
    private static final double RELIEF_TEMP_FLAT_STEP = 8.0;

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    private final PersistentDataContainer pdc;

    private PlayerData(Player player) {
        this.pdc = player.getPersistentDataContainer();
    }

    public static PlayerData of(Player player) {
        return new PlayerData(player);
    }

    public boolean hasVisitedNether() {
        return pdc.has(VISITED_NETHER, PersistentDataType.BYTE);
    }

    public boolean hasVisitedEnd() {
        return pdc.has(VISITED_END, PersistentDataType.BYTE);
    }

    public void setVisitedNether() {
        pdc.set(VISITED_NETHER, PersistentDataType.BYTE, (byte) 1);
    }

    public void setVisitedEnd() {
        pdc.set(VISITED_END, PersistentDataType.BYTE, (byte) 1);
    }

    /** OP が設定する難易度手動調整値。算出値に加算される。 */
    public double getDifficultyOffset() {
        Double value = pdc.get(DIFFICULTY_OFFSET, PersistentDataType.DOUBLE);
        return value == null ? 0.0 : value;
    }

    public void setDifficultyOffset(double value) {
        pdc.set(DIFFICULTY_OFFSET, PersistentDataType.DOUBLE, value);
    }

    // ---- 事故救済 ----

    private double getDouble(NamespacedKey k) {
        Double v = pdc.get(k, PersistentDataType.DOUBLE);
        return v == null ? 0.0 : v;
    }

    private long getLong(NamespacedKey k, long def) {
        Long v = pdc.get(k, PersistentDataType.LONG);
        return v == null ? def : v;
    }

    /** 事故救済の永続軽減率（[0,1]）。危険度に {@code (1 - これ)} が乗る。 */
    public double reliefPermanentPct() {
        return Math.min(1.0, getDouble(RELIEF_PERM));
    }

    /** 事故救済の臨時軽減率（currentDay が失効日を過ぎていれば 0）。 */
    public double reliefTempPct(long currentDay) {
        return currentDay > getLong(RELIEF_TEMP_EXPIRY, Long.MIN_VALUE) ? 0.0 : Math.min(1.0, getDouble(RELIEF_TEMP_PCT));
    }

    /** 事故救済の臨時実数軽減（失効していれば 0）。 */
    public double reliefTempFlat(long currentDay) {
        return currentDay > getLong(RELIEF_TEMP_EXPIRY, Long.MIN_VALUE) ? 0.0 : getDouble(RELIEF_TEMP_FLAT);
    }

    /**
     * 死亡を記録し、同一MC日の死亡が {@link #RELIEF_THRESHOLD} 回以上なら事故救済を加算する。
     * 永続分は全体の0.2%ずつ恒久蓄積、臨時分は実数8・割合2%を蓄積して MC1日後に失効する。
     *
     * @param currentDay プライマリワールドの経過日数（gameTime / 24000）
     */
    public void recordDeathRelief(long currentDay) {
        Integer prev = pdc.get(RELIEF_DEATHS, PersistentDataType.INTEGER);
        int deaths = (getLong(RELIEF_DAY, Long.MIN_VALUE) == currentDay && prev != null ? prev : 0) + 1;
        pdc.set(RELIEF_DEATHS, PersistentDataType.INTEGER, deaths);
        pdc.set(RELIEF_DAY, PersistentDataType.LONG, currentDay);

        // 臨時分の失効: 前回の臨時から MC1日以上経っていたら一旦クリアしてから積み直す。
        if (currentDay > getLong(RELIEF_TEMP_EXPIRY, Long.MIN_VALUE)) {
            pdc.set(RELIEF_TEMP_PCT, PersistentDataType.DOUBLE, 0.0);
            pdc.set(RELIEF_TEMP_FLAT, PersistentDataType.DOUBLE, 0.0);
        }

        if (deaths >= RELIEF_THRESHOLD) {
            pdc.set(RELIEF_PERM, PersistentDataType.DOUBLE, getDouble(RELIEF_PERM) + RELIEF_PERM_STEP);
            pdc.set(RELIEF_TEMP_PCT, PersistentDataType.DOUBLE, getDouble(RELIEF_TEMP_PCT) + RELIEF_TEMP_PCT_STEP);
            pdc.set(RELIEF_TEMP_FLAT, PersistentDataType.DOUBLE, getDouble(RELIEF_TEMP_FLAT) + RELIEF_TEMP_FLAT_STEP);
            pdc.set(RELIEF_TEMP_EXPIRY, PersistentDataType.LONG, currentDay + 1);
        }
    }
}
