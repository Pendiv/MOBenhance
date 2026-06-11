package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Player attacks have a 50% chance to whiff. */
public final class StupefactionTrait extends Trait {

    public StupefactionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("stupefaction", "STUPEFY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player && ThreadLocalRandom.current().nextDouble() < 0.5) {
            event.setCancelled(true);
        }
    }
}
