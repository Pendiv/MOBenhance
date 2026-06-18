package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;

/** 近似実装 GRENADE: ホーミングするシュルカー弾を発射し、着弾時に爆発する（ブロック破壊なし）。 */
public final class GrenadeTrait extends RangedTrait {

    private final float basePower;

    public GrenadeTrait(int cost, int weight, int maxRank, int minLevel, int cooldownTicks, float basePower) {
        super("grenade", "GREN", cost, weight, maxRank, minLevel, cooldownTicks);
        this.basePower = basePower;
    }

    /** これを超える数のシュルカー弾が周囲に飛んでいる間は発射しない（弾幕の飽和を防ぐ）。 */
    private static final int MAX_NEARBY_BULLETS = 10;

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        long nearby = mob.getNearbyEntities(24, 24, 24).stream()
                .filter(e -> e instanceof ShulkerBullet)
                .count();
        if (nearby > MAX_NEARBY_BULLETS) {
            return null; // 周囲のシュルカー弾が10を超えていたら発射不可
        }
        return mob.getWorld().spawn(mob.getEyeLocation(), ShulkerBullet.class, bullet -> bullet.setTarget(target));
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        Location loc = projectile.getLocation();
        projectile.remove();
        loc.getWorld().createExplosion(loc, basePower * rank, false, false);
    }
}
