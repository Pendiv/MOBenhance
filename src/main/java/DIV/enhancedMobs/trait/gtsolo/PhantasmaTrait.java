package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * プレイヤーから3回被弾するか、24/(rank+1) 秒のクールタイムで「幻惑」を獲得。
 * 幻惑保持中はプレイヤー由来の次のダメージを1度だけ無効化する（重複しない）。
 */
public final class PhantasmaTrait extends Trait {

    private static final String ILLUSION = "phantasma_illusion";
    private static final String HITS = "phantasma_hits";
    private static final String CD = "phantasma_cd";

    public PhantasmaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantasma", "PHANTASMA", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, ILLUSION, 0) == 1) {
            return;
        }
        int now = (int) EntityState.gameTime();
        double cd = EntityState.getDouble(mob, CD, -1);
        if (cd < 0) {
            EntityState.setDouble(mob, CD, now); // 初回はCD起点だけ記録
            return;
        }
        if (now - cd >= 24.0 / (rank + 1) * 20) {
            grant(mob, now);
        }
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return; // プレイヤー由来のみカウント・無効化
        }
        int now = (int) EntityState.gameTime();
        if (EntityState.getInt(mob, ILLUSION, 0) == 1) {
            event.setCancelled(true); // 1度だけ無効化して消費
            EntityState.setInt(mob, ILLUSION, 0);
            EntityState.setInt(mob, HITS, 0);
            EntityState.setDouble(mob, CD, now);
            return;
        }
        if (EntityState.addInt(mob, HITS, 1) >= 3) {
            grant(mob, now);
        }
    }

    private static void grant(LivingEntity mob, int now) {
        EntityState.setInt(mob, ILLUSION, 1);
        EntityState.setInt(mob, HITS, 0);
        EntityState.setDouble(mob, CD, now);
    }
}
