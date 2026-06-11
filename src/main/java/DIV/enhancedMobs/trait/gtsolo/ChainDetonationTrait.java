package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 火・溶岩・爆発ダメージを受けると即座に着火するクリーパー（連鎖爆発を誘発）。 */
public final class ChainDetonationTrait extends Trait {

    public ChainDetonationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("chain_detonation", "CHAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, BLOCK_EXPLOSION, ENTITY_EXPLOSION -> {
                if (mob instanceof Creeper creeper) {
                    creeper.setIgnited(true);
                }
            }
            default -> {
            }
        }
    }
}
