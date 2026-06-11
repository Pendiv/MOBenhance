package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Revives on lethal damage with a chance that decays each success. */
public final class EndlessTaleTrait extends Trait {

    public EndlessTaleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("endless_tale", "ENDLESS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        int revives = EntityState.getInt(mob, "et_revives", 0);
        double chance = (0.5 + 0.02 * rank) * Math.pow(0.8, revives);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            event.setCancelled(true);
            mob.setHealth(Mobs.maxHealth(mob) * 0.5);
            EntityState.setInt(mob, "et_revives", revives + 1);
        }
    }
}
