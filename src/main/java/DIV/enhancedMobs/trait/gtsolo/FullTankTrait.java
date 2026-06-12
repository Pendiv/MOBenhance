package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/** 爆発半径を拡大し火を撒くクリーパー。半径 = 3.0 × (1.5 + 0.2 × ランク)。 */
public final class FullTankTrait extends Trait {

    /** vanilla クリーパーの標準爆発半径（帯電補正を受けない基準値）。 */
    private static final float BASE_RADIUS = 3.0f;

    public FullTankTrait(int cost, int weight, int maxRank, int minLevel) {
        super("full_tank", "FTANK", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onExplosionPrime(LivingEntity mob, int rank, ExplosionPrimeEvent event) {
        // 常に基準 3.0 から再計算（原典仕様: 帯電クリーパーでも倍率は掛からない）
        event.setRadius(BASE_RADIUS * (1.5f + 0.2f * rank));
        event.setFire(true);
    }
}
