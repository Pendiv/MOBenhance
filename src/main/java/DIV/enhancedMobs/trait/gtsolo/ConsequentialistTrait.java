package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 結果論者 — スケルトン専用。CD max(20, 80-15×rank) tick で半径32の最寄りプレイヤーへ
 * 「帰結の矢」を放つ。矢は進行方向へ最大128ブロックを raycast し、最初の衝突点の手前へ
 * 瞬間配置されて次tickで着弾する（事実上のヒットスキャン）。通常AIの射撃とは独立。
 */
public final class ConsequentialistTrait extends Trait {

    private static final double SEARCH_RADIUS = 32.0;
    private static final double MAX_RANGE = 128.0;
    private static final double ARROW_SPEED = 3.0;
    private static final float INACCURACY = 0.5f;

    public ConsequentialistTrait(int cost, int weight, int maxRank, int minLevel) {
        super("consequentialist", "CONSEQ", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "cd_" + id())) {
            return;
        }
        Player target = Mobs.nearestPlayer(mob, SEARCH_RADIUS);
        if (target == null) {
            return;
        }
        EntityState.setFlag(mob, "cd_" + id(), Math.max(20, 80 - 15 * rank));

        Location eye = mob.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (dir.lengthSquared() < 1e-6) {
            return;
        }
        // 原典のばらつき0.5相当（vanilla: 軸ごとにガウス×0.0075×inaccuracy）。
        ThreadLocalRandom random = ThreadLocalRandom.current();
        dir.normalize().add(new Vector(
                random.nextGaussian() * 0.0075 * INACCURACY,
                random.nextGaussian() * 0.0075 * INACCURACY,
                random.nextGaussian() * 0.0075 * INACCURACY)).normalize();

        // ブロック+エンティティの複合 raycast。最初の衝突点の0.75手前へ矢を瞬間配置する。
        RayTraceResult hit = mob.getWorld().rayTrace(eye, dir, MAX_RANGE, FluidCollisionMode.NEVER, true, 0.1,
                e -> e != mob && e instanceof LivingEntity);
        double distance = hit != null ? hit.getHitPosition().distance(eye.toVector()) : MAX_RANGE;
        Location spawnAt = eye.clone().add(dir.clone().multiply(Math.max(0.0, distance - 0.75)));
        spawnAt.setDirection(dir);

        Arrow arrow = mob.getWorld().spawn(spawnAt, Arrow.class);
        arrow.setShooter(mob);
        arrow.setVelocity(dir.clone().multiply(ARROW_SPEED));
        TraitProjectiles.tag(arrow, id(), rank);

        // 即着弾の視認性を補う発射音 + 弾道パーティクル線。
        mob.getWorld().playSound(eye, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        for (double d = 1.0; d < distance; d += 2.0) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            mob.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
        }
    }
}
