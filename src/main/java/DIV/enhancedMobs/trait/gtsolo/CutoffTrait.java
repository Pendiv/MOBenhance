package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Negates tiny hits (below 1% of max health). */
public final class CutoffTrait extends Trait {

    public CutoffTrait(int cost, int weight, int maxRank, int minLevel) {
        super("cutoff", "CUTOFF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (event.getFinalDamage() < Mobs.maxHealth(mob) * 0.01) {
            event.setCancelled(true);
        }
    }
}
