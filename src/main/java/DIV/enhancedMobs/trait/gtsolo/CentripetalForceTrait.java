package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Creeper that drags nearby players toward itself. */
public final class CentripetalForceTrait extends Trait {

    public CentripetalForceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("centripetal_force", "CENTRI", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double range = 3 + 2 * rank;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player player) {
                Vector pull = mob.getLocation().toVector().subtract(player.getLocation().toVector());
                if (pull.lengthSquared() > 0.01) {
                    player.setVelocity(player.getVelocity().add(pull.normalize().multiply(0.4)));
                }
            }
        }
    }
}
