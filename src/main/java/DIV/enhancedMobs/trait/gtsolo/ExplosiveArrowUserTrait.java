package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * スケルトン専用。CD = max(200, 600 − 80×rank) tick（26〜18 秒）で半径32の最寄りプレイヤーへ
 * 爆裂矢を放つ。着弾点で威力 4+rank の爆発（延焼なし・ブロック破壊なし・射手に帰属）。
 */
public final class ExplosiveArrowUserTrait extends RangedTrait {

    private static final double RADIUS = 32.0;

    public ExplosiveArrowUserTrait(int cost, int weight, int maxRank, int minLevel) {
        super("explosive_arrow_user", "EXARROW", cost, weight, maxRank, minLevel, 600);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected int cooldownTicks(int rank) {
        // 原典: 30 秒基準、−4 秒/lv、下限 10 秒
        return Math.max(200, 600 - 80 * rank);
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        // 原典は AI ターゲットでなく半径32の最寄りプレイヤーを狙う
        Player player = Mobs.nearestPlayer(mob, RADIUS);
        if (player == null) {
            return null;
        }
        return shootAimedArrow(mob, player, 3.0, 0.5);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        Location loc = projectile.getLocation();
        projectile.remove();
        // 威力 4+rank、延焼なし・ブロック破壊なし。source = 射手でダメージ帰属（自爆ダメージ回避）
        loc.getWorld().createExplosion(shooter, loc, 4.0f + rank, false, false);
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
