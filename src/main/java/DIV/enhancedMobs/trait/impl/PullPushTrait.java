package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Aura that pulls nearby players toward (PULLING) or pushes them away (REPELLING). */
public final class PullPushTrait extends Trait {

    private final double range;
    private final double strength;
    private final double direction; // +1 toward the mob, -1 away

    public PullPushTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                         double range, double strength, double direction) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.range = range;
        this.strength = strength;
        this.direction = direction;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            Vector pull = mob.getLocation().toVector().subtract(player.getLocation().toVector());
            if (pull.lengthSquared() < 0.01) {
                continue;
            }
            pull.normalize().multiply(direction * strength);
            player.setVelocity(player.getVelocity().add(pull));
        }
    }
}
