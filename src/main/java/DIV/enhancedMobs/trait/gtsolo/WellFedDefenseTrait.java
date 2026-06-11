package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Takes less damage from hungrier attackers (scaled by attacker food level). */
public final class WellFedDefenseTrait extends Trait {

    public WellFedDefenseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("well_fed_defense", "WFDEF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player player) {
            event.setDamage(event.getDamage() * (player.getFoodLevel() / 20.0));
        }
    }
}
