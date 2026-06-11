package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/** スケルトン専用: 命中時に爆発する矢を射つ（ブロックダメージなし）。 */
public final class ExplosiveArrowUserTrait extends RangedTrait {

    public ExplosiveArrowUserTrait(int cost, int weight, int maxRank, int minLevel) {
        super("explosive_arrow_user", "EXARROW", cost, weight, maxRank, minLevel, 60);
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
        loc.getWorld().createExplosion(loc, 1.0f + 0.5f * rank, false, false);
    }
}
