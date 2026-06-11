package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Caps incoming player damage at 1.0, negating weapon/enchant bonuses. */
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
