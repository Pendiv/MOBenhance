package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/** 致死ダメージ時に確率で復活し、復活成功のたびに次回の確率が0.8倍に減衰する。 */
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
