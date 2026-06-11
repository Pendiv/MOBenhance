package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/** Skeleton fires arrows that call down lightning on impact. */
public final class LightningUserTrait extends RangedTrait {

    public LightningUserTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lightning_user", "LIGHT", cost, weight, maxRank, minLevel, 60);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 2.5);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        Location loc = projectile.getLocation();
        projectile.remove();
        loc.getWorld().strikeLightning(loc);
    }
}
