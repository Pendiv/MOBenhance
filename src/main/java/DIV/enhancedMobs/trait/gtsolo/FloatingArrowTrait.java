package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * スケルトン専用。半径32の最寄りプレイヤーが自分より +2.0 超の高所にいる間のみ:
 * 自身に浮遊Iを維持して追従し、60t CD で浮遊矢を放つ。矢の命中で浮遊II（60t）
 * + 上向き打ち上げ（vy ≥ 0.8）。矢の通常ダメージはそのまま通る。rank 非依存（原典準拠）。
 */
public final class FloatingArrowTrait extends Trait {

    private static final double RADIUS = 32.0;
    private static final double HEIGHT_MARGIN = 2.0;
    private static final int COOLDOWN = 60;

    public FloatingArrowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("floating_arrow", "FLOAT", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        Player target = Mobs.nearestPlayer(mob, RADIUS);
        if (target == null
                || target.getLocation().getY() <= mob.getLocation().getY() + HEIGHT_MARGIN) {
            return; // プレイヤーが高所にいる時のみ発動
        }
        // 自身に浮遊Iを維持して高所のプレイヤーへ追従（tick 間隔より長い duration で途切れさせない）
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,
                Math.max(40, interval * 2), 0, false, false));
        if (EntityState.hasFlag(mob, "cd_floating_arrow")) {
            return;
        }
        EntityState.setFlag(mob, "cd_floating_arrow", COOLDOWN);
        Arrow arrow = shootAimedArrow(mob, target, 3.0, 0.5);
        TraitProjectiles.tag(arrow, id(), rank);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity hit)) {
            return;
        }
        // 原典 SWALLOW perf 2.0: 浮遊II固定（rank 非依存）+ 上向き打ち上げ。
        // 矢の通常ダメージは生かす（remove しない）
        hit.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1, true, true, true));
        Vector v = hit.getVelocity();
        hit.setVelocity(new Vector(v.getX(), Math.max(v.getY(), 0.8), v.getZ()));
    }

    /** 原典 SpecialArrow.aimedArrow 相当: 弧補正（水平距離×0.2）+ ばらつき付きの照準矢。 */
    private static Arrow shootAimedArrow(LivingEntity mob, LivingEntity target, double speed, double inaccuracy) {
        Location eye = mob.getEyeLocation();
        double dx = target.getLocation().getX() - mob.getLocation().getX();
        double dy = target.getLocation().getY() + target.getHeight() / 3.0 - eye.getY();
        double dz = target.getLocation().getZ() - mob.getLocation().getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        Vector dir = new Vector(dx, dy + horiz * 0.2, dz);
        if (dir.lengthSquared() < 1e-6) {
            dir = mob.getLocation().getDirection();
        }
        dir.normalize();
        // vanilla shoot() の三角分布ばらつき近似
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double spread = 0.0172275 * inaccuracy;
        dir.add(new Vector((random.nextDouble() - random.nextDouble()) * spread,
                (random.nextDouble() - random.nextDouble()) * spread,
                (random.nextDouble() - random.nextDouble()) * spread));
        Arrow arrow = mob.getWorld().spawn(eye, Arrow.class);
        arrow.setShooter(mob);
        arrow.setVelocity(dir.multiply(speed));
        return arrow;
    }
}
