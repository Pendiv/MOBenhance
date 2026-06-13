package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * 初めて受けるタイプのダメージを100%軽減し、同タイプを受けるたびに軽減率が低下する
 * （原典 RejectionUnknown。「適応されると弱くなる」方向 — 旧移植は方向が反転していた）。
 *
 * <p>軽減率 = max(0, 1 - count × step)、step = 0.24÷(rank+1)（lv1: 1回毎 -12%、lv3: -6%）。
 * DamageCause 別の被弾回数を PDC に永続化し、種類を問わず最後の被弾から 600 tick（30秒）
 * 経過で全カウントをリセットする。
 */
public final class RejectionUnknownTrait extends Trait {

    private static final int RESET_TICKS = 600;
    private static final String COUNTS_KEY = "reject_counts";
    private static final String LAST_HIT_KEY = "reject_last";

    public RejectionUnknownTrait(int cost, int weight, int maxRank, int minLevel) {
        super("rejection_unknown", "REJECT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        int now = (int) EntityState.gameTime();
        // 種類を問わず最後の被弾から30秒経過していれば適応をリセット。
        Map<String, Integer> counts = now - EntityState.getInt(mob, LAST_HIT_KEY, -1_000_000) > RESET_TICKS
                ? new HashMap<>()
                : readCounts(mob);
        EntityState.setInt(mob, LAST_HIT_KEY, now);

        String cause = event.getCause().name();
        int count = counts.getOrDefault(cause, 0);
        // 初見（count=0）→ ダメージ0。被弾を重ねるごとに step ずつ通るようになる。
        double step = 0.24 / (rank + 1);
        event.setDamage(event.getDamage() * Math.min(1.0, count * step));
        counts.put(cause, count + 1);
        writeCounts(mob, counts);
    }

    /** PDC の "CAUSE:n;CAUSE:n" 形式文字列を読み出す（原典の damage type 別 Map に対応）。 */
    private static Map<String, Integer> readCounts(LivingEntity mob) {
        Map<String, Integer> counts = new HashMap<>();
        String raw = mob.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return counts;
        }
        for (String entry : raw.split(";")) {
            int sep = entry.lastIndexOf(':');
            if (sep > 0) {
                try {
                    counts.put(entry.substring(0, sep), Integer.parseInt(entry.substring(sep + 1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return counts;
    }

    private static void writeCounts(LivingEntity mob, Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        counts.forEach((cause, count) -> {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(cause).append(':').append(count);
        });
        mob.getPersistentDataContainer().set(key(), PersistentDataType.STRING, sb.toString());
    }

    private static NamespacedKey key() {
        return new NamespacedKey(EnhancedMobs.get(), COUNTS_KEY);
    }
}
