package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

/**
 * クリーパー専用。視線・遮蔽を問わず半径8内にプレイヤーが居続けた滞在時間を累積し、
 * 閾値 max(20, 120 - 20×rank) tick 到達で着火する（圏外に出たら0リセット）。
 */
public final class ProximityFuseTrait extends Trait {

    private static final String DWELL = "fuse_dwell";
    private static final double RADIUS = 8.0;

    public ProximityFuseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("proximity_fuse", "PROXFUSE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Creeper creeper) || creeper.isIgnited()) {
            return; // 既に着火中は何もしない
        }
        if (Mobs.nearestPlayer(mob, RADIUS) == null) {
            EntityState.setInt(mob, DWELL, 0);
            return;
        }
        // tick はtick-interval間隔で呼ばれるため、滞在時間はその間隔ぶんまとめて加算する
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        int dwell = EntityState.addInt(mob, DWELL, interval);
        if (dwell >= Math.max(20, 120 - 20 * rank)) {
            creeper.setIgnited(true);
            EntityState.setInt(mob, DWELL, 0);
        }
    }
}
