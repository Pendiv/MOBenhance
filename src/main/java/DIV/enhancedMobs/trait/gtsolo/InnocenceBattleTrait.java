package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤー以外からのダメージを無効化する。 */
public final class InnocenceBattleTrait extends Trait {

    public InnocenceBattleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("innocence_battle", "INNOCENCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            event.setCancelled(true);
        }
    }
}
