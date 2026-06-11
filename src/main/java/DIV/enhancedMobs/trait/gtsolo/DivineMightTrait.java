package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーからの被ダメージを最大1.0に制限し、武器・エンチャントによる過剰ダメージを無効化する。 */
public final class DivineMightTrait extends Trait {

    public DivineMightTrait(int cost, int weight, int maxRank, int minLevel) {
        super("divine_might", "DIVINE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            event.setDamage(Math.min(event.getDamage(), 1.0));
        }
    }
}
