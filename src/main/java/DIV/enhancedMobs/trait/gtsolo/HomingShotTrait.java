package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * スケルトン専用・パッシブ。自分が放った全ての矢（通常攻撃の矢を含む）に毎 tick の追尾補正をかける:
 * {@code newVel = normalize(vel×0.98 + dir×(speed×0.02)) × speed}（速度保存・補正率は rank 非依存）。
 * 追尾対象は半径48の最寄りプレイヤーの胴中心。地面に刺さるか消滅で解除。
 */
public final class HomingShotTrait extends Trait {

    private static final double SEARCH_RADIUS = 48.0;
    /** 軌道補正率（原典 0.02 固定）。 */
    private static final double CORRECTION = 0.02;
    /** 発射直後の自分の矢を拾う走査半径（FastTick は毎 tick なので初速 3.0 でも取り逃さない）。 */
    private static final double PICKUP_RADIUS = 8.0;

    public HomingShotTrait(int cost, int weight, int maxRank, int minLevel) {
        super("homing_shot", "HOMING", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        ensureRegistered(mob);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // チャンク再ロード等で FastTick 登録が消えた場合の再登録
        ensureRegistered(mob);
    }

    /** 原典の EntityJoinLevelEvent 相当: 毎 tick、射出直後の自分の矢を検出して追尾を開始する。 */
    private static void ensureRegistered(LivingEntity mob) {
        if (FastTick.isRegistered(mob, "homing_shot")) {
            return;
        }
        FastTick.register(mob, "homing_shot", () -> {
            if (!mob.isValid()) {
                return false;
            }
            for (Entity entity : mob.getNearbyEntities(PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS)) {
                if (entity instanceof AbstractArrow arrow
                        && !arrow.isDead()
                        && !arrow.isInBlock()
                        && !FastTick.isRegistered(arrow, "homing_steer")
                        && arrow.getShooter() instanceof LivingEntity shooter
                        && shooter.getUniqueId().equals(mob.getUniqueId())) {
                    steer(arrow);
                }
            }
            return true;
        });
    }

    /** 矢ごとの毎 tick 追尾補正（速度保存ブレンド）。刺さる・消えるまで継続。 */
    private static void steer(AbstractArrow arrow) {
        FastTick.register(arrow, "homing_steer", () -> {
            if (!arrow.isValid() || arrow.isDead() || arrow.isInBlock()) {
                return false;
            }
            Vector vel = arrow.getVelocity();
            double speed = vel.length();
            if (speed * speed < 1.0e-4) {
                return false; // 地面に刺さった
            }
            Player target = nearestPlayer(arrow);
            if (target == null) {
                return true;
            }
            Vector desired = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector()
                    .subtract(arrow.getLocation().toVector());
            if (desired.lengthSquared() < 1.0e-6) {
                return true;
            }
            desired.normalize();
            Vector blended = vel.multiply(1.0 - CORRECTION).add(desired.multiply(speed * CORRECTION));
            if (blended.lengthSquared() < 1.0e-6) {
                return true;
            }
            arrow.setVelocity(blended.normalize().multiply(speed));
            return true;
        });
    }

    private static Player nearestPlayer(AbstractArrow arrow) {
        Player best = null;
        double bestSq = SEARCH_RADIUS * SEARCH_RADIUS;
        for (Player player : arrow.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double d = player.getLocation().distanceSquared(arrow.getLocation());
            if (d < bestSq) {
                bestSq = d;
                best = player;
            }
        }
        return best;
    }
}
