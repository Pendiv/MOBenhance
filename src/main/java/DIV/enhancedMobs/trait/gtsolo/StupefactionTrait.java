package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 白痴 — 保持者が16m以内にいるプレイヤーの攻撃（近接・矢とも）は50%で外れる。
 * 保持者自身への攻撃は {@link #onAttackedBy} で完結。他Mobへの攻撃の保護は
 * {@link #tick} で周囲プレイヤーに付けるフラグ {@code stupefaction_zone} を
 * ダメージ側でチェックする配線が必要（MobListener 側）。
 */
public final class StupefactionTrait extends Trait {

    private static final double RANGE = 16.0;
    private static final double MISS_CHANCE = 0.5;

    public StupefactionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("stupefaction", "STUPEFY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 16m内のプレイヤーへ「白痴圏内」フラグを付与（次tickまで+余裕分）。
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        for (Entity entity : mob.getNearbyEntities(RANGE, RANGE, RANGE)) {
            if (entity instanceof Player player) {
                EntityState.setFlag(player, "stupefaction_zone", interval + 10);
            }
        }
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        // 原典: プレイヤー周囲16m（箱形）に保持者がいれば50%でミス。遠距離狙撃（>16m）は無効化されない。
        if (!(attacker instanceof Player) || attacker.getWorld() != mob.getWorld()) {
            return;
        }
        Location a = attacker.getLocation();
        Location b = mob.getLocation();
        if (Math.abs(a.getX() - b.getX()) > RANGE || Math.abs(a.getY() - b.getY()) > RANGE
                || Math.abs(a.getZ() - b.getZ()) > RANGE) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < MISS_CHANCE) {
            event.setCancelled(true);
        }
    }
}
