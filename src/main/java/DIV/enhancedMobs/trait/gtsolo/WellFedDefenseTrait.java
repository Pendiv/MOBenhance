package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃者の食料レベルに比例してダメージを軽減する（満腹=等倍、空腹で大幅減）。 */
public final class WellFedDefenseTrait extends Trait {

    /** 空腹時でも残る最低ダメージ割合（完全無敵＝倒せなくなるのを防ぐ）。 */
    private static final double MIN_FACTOR = 0.2;

    public WellFedDefenseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("well_fed_defense", "WFDEF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player player) {
            double factor = Math.max(MIN_FACTOR, player.getFoodLevel() / 20.0);
            event.setDamage(event.getDamage() * factor);
        }
    }
}
