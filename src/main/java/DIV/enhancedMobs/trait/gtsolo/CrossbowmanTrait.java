package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/** 命中時に強烈なノックバックを与える矢を射るスケルトン。 */
public final class CrossbowmanTrait extends RangedTrait {

    public CrossbowmanTrait(int cost, int weight, int maxRank, int minLevel) {
        super("crossbowman", "XBOW", cost, weight, maxRank, minLevel, 40);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 3.5);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof LivingEntity hit && TraitCooldown.ready(hit, "crossbowman", 30, 15, 4, 200)) {
            hit.setVelocity(hit.getVelocity().add(projectile.getVelocity().normalize().multiply(0.4 + 0.2 * rank)));
        }
    }
}
