package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃時にプレイヤーの食料レベルをランク分減らす。 */
public final class HungerDrainTrait extends Trait {

    public HungerDrainTrait(int cost, int weight, int maxRank, int minLevel) {
        super("hunger_drain", "HUNGER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target instanceof Player player) {
            player.setFoodLevel(Math.max(0, player.getFoodLevel() - rank));
        }
    }
}
