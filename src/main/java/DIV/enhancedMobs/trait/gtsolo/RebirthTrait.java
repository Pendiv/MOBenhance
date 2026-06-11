package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーから受けた最終ダメージの10%×ランク分を回復する。 */
public final class RebirthTrait extends Trait {

    public RebirthTrait(int cost, int weight, int maxRank, int minLevel) {
        super("rebirth", "REBIRTH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            double heal = event.getFinalDamage() * 0.1 * rank;
            mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + heal));
        }
    }
}
