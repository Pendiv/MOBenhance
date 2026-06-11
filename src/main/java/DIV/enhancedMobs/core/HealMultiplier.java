package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * プラグイン独自の回復量倍率（AttributesLib の同名属性に倣い、基底値 1.0、0 = 回復なし）。
 * Bukkit はカスタム属性をランタイム登録できないため PDC に保持し、
 * {@code HealListener} が EntityRegainHealthEvent で適用する。
 *
 * <p>恒久的な基底倍率と、CURSED が使う時限「呪い」上書きをサポート。
 */
public final class HealMultiplier {

    private static final NamespacedKey BASE = key("heal_base");
    private static final NamespacedKey CURSE_MULT = key("heal_curse_mult");
    private static final NamespacedKey CURSE_UNTIL = key("heal_curse_until");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }

    private HealMultiplier() {
    }

    /** 現在の実効回復倍率（1.0 = 変化なし）。 */
    public static double effective(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        double factor = pdc.getOrDefault(BASE, PersistentDataType.DOUBLE, 1.0);
        Long until = pdc.get(CURSE_UNTIL, PersistentDataType.LONG);
        if (until != null && Bukkit.getCurrentTick() < until) {
            factor *= pdc.getOrDefault(CURSE_MULT, PersistentDataType.DOUBLE, 1.0);
        }
        return factor;
    }

    /** 回復量に時限倍率を付与（CURSED 用）。mult 0 で期間中の回復を完全封鎖。 */
    public static void applyCurse(LivingEntity entity, double mult, int durationTicks) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(CURSE_MULT, PersistentDataType.DOUBLE, mult);
        pdc.set(CURSE_UNTIL, PersistentDataType.LONG, (long) Bukkit.getCurrentTick() + durationTicks);
    }
}
