package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

/** Creeper with a much larger explosion radius. */
public final class FullTankTrait extends Trait {

    public FullTankTrait(int cost, int weight, int maxRank, int minLevel) {
        super("full_tank", "FTANK", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (mob instanceof Creeper creeper) {
            creeper.setExplosionRadius((int) Math.ceil(creeper.getExplosionRadius() * (1.5 + 0.2 * rank)));
        }
    }
}
