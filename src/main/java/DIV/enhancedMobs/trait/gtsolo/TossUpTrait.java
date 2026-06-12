package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** プレイヤーへの攻撃命中時、上方へ打ち上げる。クールダウン付き（体勢崩しスパム防止）。 */
public final class TossUpTrait extends Trait {

    public TossUpTrait(int cost, int weight, int maxRank, int minLevel) {
        super("toss_up", "TOSS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player)) {
            return;
        }
        if (EntityState.hasFlag(mob, "toss_cd")) {
            return;
        }
        // 原典: CD = max(12, 60/rank) tick。
        EntityState.setFlag(mob, "toss_cd", Math.max(12, 60 / Math.max(1, rank)));
        // 原典: Y速度を 1.2 まで引き上げ（上昇中なら現在値を維持、X/Zは不変）。
        Vector v = target.getVelocity();
        target.setVelocity(v.setY(Math.max(v.getY(), 1.2)));
    }
}
