package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

/** Creeper that auto-ignites when a player is close. */
public final class ProximityFuseTrait extends Trait {

    public ProximityFuseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("proximity_fuse", "PROXFUSE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob instanceof Creeper creeper && Mobs.nearestPlayer(mob, 3 + rank) != null) {
            creeper.setIgnited(true);
        }
    }
}
