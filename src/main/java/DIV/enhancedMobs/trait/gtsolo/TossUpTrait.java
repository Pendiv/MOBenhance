package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 胴上げ: プレイヤーへの攻撃命中時、足元でウィンドチャージを炸裂させて打ち上げる。
 * クールダウン付き（体勢崩しスパム防止）。
 */
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
        // 胴上げ = ウィンドチャージ: 対象の足元で炸裂させると突風で上方へ打ち上がる。
        WindCharge charge = target.getWorld().spawn(target.getLocation(), WindCharge.class,
                wc -> wc.setShooter(mob));
        charge.explode();
    }
}
