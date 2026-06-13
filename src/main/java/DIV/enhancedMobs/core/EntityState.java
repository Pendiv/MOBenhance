package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * 特性用の汎用エンティティ状態管理。時限フラグ（クールダウン・ウィンドウ）と
 * 累積数値（スタック・倍率）を PDC に保存。エンティティ・プレイヤー両方で動作する。
 */
public final class EntityState {

    private EntityState() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    /**
     * サーバー全体で単調増加する基準クロック（プライマリワールドの gameTime）。
     * {@code Bukkit.getCurrentTick()} はサーバー再起動で 0 に戻るため、PDC に保存する
     * 期限値の基準には使えない（再起動後に CD が数時間スタックしたり、使い切りフラグが
     * 復活する）。gameTime は level.dat に永続し、/time set でも巻き戻らない。
     */
    public static long gameTime() {
        return Bukkit.getWorlds().get(0).getGameTime();
    }

    private static long now() {
        return gameTime();
    }

    /** 指定 tick 間アクティブなフラグをセット（クールダウンとしても利用）。 */
    public static void setFlag(PersistentDataHolder holder, String name, int durationTicks) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.LONG,
                now() + durationTicks);
    }

    public static boolean hasFlag(PersistentDataHolder holder, String name) {
        Long until = holder.getPersistentDataContainer().get(key(name), PersistentDataType.LONG);
        return until != null && now() < until;
    }

    public static double getDouble(PersistentDataHolder holder, String name, double def) {
        Double value = holder.getPersistentDataContainer().get(key(name), PersistentDataType.DOUBLE);
        return value == null ? def : value;
    }

    public static void setDouble(PersistentDataHolder holder, String name, double value) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.DOUBLE, value);
    }

    public static double addDouble(PersistentDataHolder holder, String name, double delta) {
        double value = getDouble(holder, name, 0) + delta;
        setDouble(holder, name, value);
        return value;
    }

    public static int getInt(PersistentDataHolder holder, String name, int def) {
        Integer value = holder.getPersistentDataContainer().get(key(name), PersistentDataType.INTEGER);
        return value == null ? def : value;
    }

    public static void setInt(PersistentDataHolder holder, String name, int value) {
        holder.getPersistentDataContainer().set(key(name), PersistentDataType.INTEGER, value);
    }

    public static int addInt(PersistentDataHolder holder, String name, int delta) {
        int value = getInt(holder, name, 0) + delta;
        setInt(holder, name, value);
        return value;
    }
}
