package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 爆発ダメージを受けるたびに爆発半径が増加するクリーパー。 */
public final class AccumulationTrait extends Trait {

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
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> {
                if (mob instanceof Creeper creeper) {
                    creeper.setExplosionRadius(Math.min(12, creeper.getExplosionRadius() + 1));
                }
            }
            default -> {
            }
        }
    }
}
