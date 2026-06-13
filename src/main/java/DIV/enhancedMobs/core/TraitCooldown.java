package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * プレイヤー×特性ごとの段階的クールダウン。オーラ・位置交換・連続ノックバック等の
 * 「回避不可」特性によるチェインロック防止用。一度被弾するとクールダウンが発生し、
 * リセット前に再び被弾すると次回がさらに延長される（上限あり）。
 * 十分な間隔を置くと段階がリセットされる。
 *
 * <p>プレイヤー以外は常に true を返す（保護対象外）。
 */
public final class TraitCooldown {

    private TraitCooldown() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    /** 簡易呼び出し: 基底 2s、連続被弾ごとに +1s、上限 5 スタック、10s でリセット。 */
    public static boolean ready(LivingEntity target, String traitId) {
        return ready(target, traitId, 40, 20, 5, 200);
    }

    /**
     * @return 特性が今この対象に作用できれば true。
     *         プレイヤーで true の場合、被弾を記録して段階的クールダウンをセット（または延長）する。
     */
    public static boolean ready(LivingEntity target, String traitId, int baseTicks, int stepTicks,
                                int maxStacks, int resetWindowTicks) {
        if (!(target instanceof Player)) {
            return true;
        }
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        NamespacedKey untilKey = key("ct_" + traitId + "_until");
        NamespacedKey lastKey = key("ct_" + traitId + "_last");
        NamespacedKey stackKey = key("ct_" + traitId + "_stk");

        // 再起動安全な基準クロック（getCurrentTick はサーバー再起動で 0 に戻り CD が破綻する）。
        long now = EntityState.gameTime();
        Long until = pdc.get(untilKey, PersistentDataType.LONG);
        if (until != null && now < until) {
            return false;
        }

        long last = pdc.getOrDefault(lastKey, PersistentDataType.LONG, -1_000_000L);
        int stacks = pdc.getOrDefault(stackKey, PersistentDataType.INTEGER, 0);
        if (now - last > resetWindowTicks) {
            stacks = 0;
        }
        int cooldown = baseTicks + Math.min(stacks, maxStacks) * stepTicks;
        pdc.set(untilKey, PersistentDataType.LONG, now + cooldown);
        pdc.set(lastKey, PersistentDataType.LONG, now);
        pdc.set(stackKey, PersistentDataType.INTEGER, stacks + 1);
        return true;
    }
}
