package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.ShulkerBullet;

/** Periodically fires a homing shulker bullet at the mob's target. */
public final class ShulkerTrait extends Trait {

    public ShulkerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("shulker", "SHULK", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Mob asMob)) {
            return;
        }
        LivingEntity target = asMob.getTarget();
        if (target == null) {
            return;
        }
        mob.getWorld().spawn(mob.getEyeLocation(), ShulkerBullet.class, bullet -> bullet.setTarget(target));
    }
}
