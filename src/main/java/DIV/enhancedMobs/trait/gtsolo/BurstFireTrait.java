package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * スケルトン専用。戦闘中は (10+5N) 発のバーストとして tick ごとに追加照準矢を放ち、
 * 撃ち切ると (75−10N) 秒のクールタイムに入る（その間は vanilla 弓 AI のみ = 緩急の波）。
 * 非戦闘時はバーストを再装填する（原典の状態機械を追加矢方式で近似。AI 自体の加速は API 外）。
 */
public final class BurstFireTrait extends Trait {

    public BurstFireTrait(int cost, int weight, int maxRank, int minLevel) {
        super("burst_fire", "BURST", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Mob asMob)) {
            return;
        }
        int burstShots = 10 + 5 * rank;
        LivingEntity target = asMob.getTarget();
        if (target == null || target.isDead()) {
            // 非戦闘: 再装填（原典と同じリセット）
            EntityState.setInt(mob, "burst_left", burstShots);
            return;
        }
        if (EntityState.hasFlag(mob, "burst_cd")) {
            return; // クールタイム中は vanilla 弓 AI のみ
        }
        int left = EntityState.getInt(mob, "burst_left", burstShots);
        if (left <= 0) {
            left = burstShots; // クールタイム明けの再バースト開始
        }
        shootAimedArrow(asMob, target, 3.0, 0.5);
        left--;
        if (left <= 0) {
            // 撃ち切り → クールタイム開始 + 次バースト分を再装填
            EntityState.setInt(mob, "burst_left", burstShots);
            EntityState.setFlag(mob, "burst_cd", Math.max(20, (75 - 10 * rank) * 20));
        } else {
            EntityState.setInt(mob, "burst_left", left);
        }
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
