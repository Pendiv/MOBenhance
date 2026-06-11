package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** Periodically spawns weaker clones of itself. */
public final class MonotoneCloneTrait extends Trait {

    public MonotoneCloneTrait(int cost, int weight, int maxRank, int minLevel) {
        super("monotone_clone", "CLONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "clone_cd")) {
            return;
        }
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel >= 1) {
            Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
            if (copy instanceof LivingEntity living) {
                EnhancedMobs.get().initializeMob(living, childLevel);
            }
        }
        EntityState.setFlag(mob, "clone_cd", 200);
    }
}
