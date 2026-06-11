package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーからの被ダメージを攻撃者の現在HP割合で倍率補正する。 */
public final class DistantHorizonTrait extends Trait {

    public DistantHorizonTrait(int cost, int weight, int maxRank, int minLevel) {
        super("distant_horizon", "HORIZON", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            event.setDamage(event.getDamage() * Mobs.healthRatio(attacker));
        }
    }
}
