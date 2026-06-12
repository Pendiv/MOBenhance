package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/** 被ダメージ時に確率で近距離テレポートする。 */
public final class EnderTrait extends Trait {

    private final double range;

    public EnderTrait(int cost, int weight, int maxRank, int minLevel, double range) {
        super("ender", "ENDER", cost, weight, maxRank, minLevel);
        this.range = range;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > 0.5) {
            return;
        }
        double dx = (random.nextDouble() * 2 - 1) * range;
        double dz = (random.nextDouble() * 2 - 1) * range;
        Location target = mob.getLocation().add(dx, 0, dz);
        Mobs.teleport(mob, target);
    }
}
