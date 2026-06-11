package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Skeleton fires arrows that apply levitation on hit. */
public final class FloatingArrowTrait extends RangedTrait {

    public FloatingArrowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("floating_arrow", "FLOAT", cost, weight, maxRank, minLevel, 50);
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
        if (event.getHitEntity() instanceof LivingEntity hit) {
            hit.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, rank - 1, true, true, true));
        }
        projectile.remove();
    }
}
