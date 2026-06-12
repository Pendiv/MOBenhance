package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーから受けたダメージ（軽減前）の12%×ランク分を回復する。 */
public final class RebirthTrait extends Trait {

    /** 原典: 被ダメの 12n% を回復（lv1=12%, lv5=60%）。 */
    private static final double HEAL_RATIO_PER_RANK = 0.12;

    public RebirthTrait(int cost, int weight, int maxRank, int minLevel) {
        super("rebirth", "REBIRTH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return;
        }
        // 原典は防具軽減前の量（heal() 経由で回復阻害フックを尊重）に対応させる
        double taken = event.getDamage();
        if (taken <= 0) {
            return;
        }
        Mobs.heal(mob, taken * HEAL_RATIO_PER_RANK * rank);
    }
}
