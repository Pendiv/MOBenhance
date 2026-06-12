package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * クリーパー専用。CD = 120 + 480/rank tick ごとに自然相場レベルのクリーパーを1体召喚し、
 * 90%で同rankの本特性を継承させる。半径32にクリーパー8体以上で召喚スキップ（CDのみリセット）。
 */
public final class SummonKinTrait extends Trait {

    private static final double INHERIT_CHANCE = 0.9;
    /** 過密抑止: 半径32（自身含む）にこの数のクリーパーが居たら召喚スキップ。 */
    private static final int MAX_NEARBY_CREEPERS = 8;

    public SummonKinTrait(int cost, int weight, int maxRank, int minLevel) {
        super("summon_kin", "SUMKIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    /** 原典: CD = 120 + 480/lv tick（lv1=600t/30s、lv4=240t/12s）。 */
    private static int cooldownTicks(int rank) {
        return 120 + 480 / Math.max(1, rank);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典: 新生個体は初回CD経過後に初発動（スポーン直後の即増殖を防ぐ）。
        EntityState.setFlag(mob, "kin_cd", cooldownTicks(rank));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "kin_cd")) {
            return;
        }
        EntityState.setFlag(mob, "kin_cd", cooldownTicks(rank));
        // 過密抑止（自身も数に含める）。上限到達時もCDは立て直す（原典のCDリセットと同じ）。
        long nearbyCreepers = 1 + mob.getNearbyEntities(32, 32, 32).stream()
                .filter(e -> e.getType() == EntityType.CREEPER)
                .count();
        if (nearbyCreepers >= MAX_NEARBY_CREEPERS) {
            return;
        }
        Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), EntityType.CREEPER);
        if (!(copy instanceof LivingEntity living)) {
            return;
        }
        EnhancedMobs plugin = EnhancedMobs.get();
        // 原典: 子は自然 init（スポーン地点の相場レベル）。
        int natural = plugin.mobBonus().apply(EntityType.CREEPER,
                plugin.difficulty().compute(living.getLocation()));
        plugin.initializeMob(living, natural);
        // 子にもCDを即時付与 — 初回CD経過まで発動不可（連鎖即増殖の防止、原典と同じ抑止構造）。
        EntityState.setFlag(living, "kin_cd", cooldownTicks(rank));
        // 自然抽選で付いた分は一旦除去し、原典どおり90%で同rankを継承させる（安全装置を維持した継承）。
        plugin.traits().stripTrait(living, "summon_kin");
        if (ThreadLocalRandom.current().nextDouble() < INHERIT_CHANCE) {
            plugin.traits().addTrait(living, "summon_kin", rank);
        }
    }
}
