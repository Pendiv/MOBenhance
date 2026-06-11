package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;

/** Compromise GRENADE: fires a homing shulker bullet that explodes on impact (no block damage). */
public final class GrenadeTrait extends RangedTrait {

    private final float basePower;

    public GrenadeTrait(int cost, int weight, int maxRank, int minLevel, int cooldownTicks, float basePower) {
        super("grenade", "GREN", cost, weight, maxRank, minLevel, cooldownTicks);
        this.basePower = basePower;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return mob.getWorld().spawn(mob.getEyeLocation(), ShulkerBullet.class, bullet -> bullet.setTarget(target));
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        Location loc = projectile.getLocation();
        projectile.remove();
        loc.getWorld().createExplosion(loc, basePower * rank, false, false);
    }
}
