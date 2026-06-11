package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Chance to fully parry a player attack. */
public final class JustParryTrait extends Trait {

    public JustParryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("just_parry", "PARRY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player && ThreadLocalRandom.current().nextDouble() < 0.3 + 0.07 * rank) {
            event.setCancelled(true);
        }
    }
}
