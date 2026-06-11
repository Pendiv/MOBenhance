package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Heals from player damage; counters hard every fifth hit. */
public final class DistantDeathTrait extends Trait {

    public DistantDeathTrait(int cost, int weight, int maxRank, int minLevel) {
        super("distant_death", "DISTDTH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return;
        }
        double heal = event.getFinalDamage() * (0.15 + 0.06 * rank);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + heal));
        if (EntityState.addInt(mob, "dd_hits", 1) % 5 == 0) {
            attacker.damage(10.0 * (0.7 + 0.15 * rank), mob);
        }
    }
}
