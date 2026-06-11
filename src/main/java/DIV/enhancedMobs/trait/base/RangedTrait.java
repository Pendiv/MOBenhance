package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

/**
 * Base for traits that fire a projectile at the mob's target on a cooldown (the many Skeleton
 * "special arrow" traits, GRENADE, etc.). Subclasses implement {@link #launch} to spawn/aim the
 * projectile and override {@link #onProjectileHit} for impact behaviour.
 */
public abstract class RangedTrait extends Trait {

    private final int cooldownTicks;

    protected RangedTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                          int cooldownTicks) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public final void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Mob asMob)) {
            return;
        }
        LivingEntity target = asMob.getTarget();
        if (target == null) {
            return;
        }
        String cooldown = "cd_" + id();
        if (EntityState.hasFlag(mob, cooldown)) {
            return;
        }
        Projectile projectile = launch(asMob, target, rank);
        if (projectile != null) {
            TraitProjectiles.tag(projectile, id(), rank);
        }
        EntityState.setFlag(mob, cooldown, cooldownTicks);
    }

    /** Spawn and aim the projectile. Return it (it will be tagged), or null to fire nothing. */
    protected abstract Projectile launch(Mob mob, LivingEntity target, int rank);
}
