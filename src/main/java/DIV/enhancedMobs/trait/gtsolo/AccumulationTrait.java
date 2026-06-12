package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * クリーパー専用。爆発ダメージを受けるたび被弾カウントを永続加算（PDC）し、
 * 自爆の起爆時に半径 = min(20, 3.0 × (1 + 0.10 × (被弾数 + rank))) へ拡大する。
 * 原典どおり rank ぶん最初から被爆済み扱い（例: rank3 無被弾で半径 3.9）。
 */
public final class AccumulationTrait extends Trait {

    /** vanilla クリーパーの標準爆発半径。 */
    private static final float BASE_RADIUS = 3.0f;
    /** 被弾 1 回ごとの半径増加率（base 比 +10%）。 */
    private static final float RANGE_PER_HIT = 0.10f;
    private static final float MAX_RADIUS = 20.0f;

    public AccumulationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("accumulation", "ACCUM", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> EntityState.addInt(mob, "accum_hits", 1);
            default -> {
            }
        }
    }

    @Override
    public void onExplosionPrime(LivingEntity mob, int rank, ExplosionPrimeEvent event) {
        // 原典の「cancel→拡大半径で再 explode」を ExplosionPrimeEvent.setRadius で再現
        int stacks = EntityState.getInt(mob, "accum_hits", 0) + rank;
        event.setRadius(Math.min(MAX_RADIUS, BASE_RADIUS * (1.0f + RANGE_PER_HIT * stacks)));
    }
}
