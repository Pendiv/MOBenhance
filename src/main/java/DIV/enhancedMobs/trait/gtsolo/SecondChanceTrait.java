package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Creeper that survives a lethal hit once, restored to full health. */
public final class SecondChanceTrait extends Trait {

    public SecondChanceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("second_chance", "2NDCHANCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "sc_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        EntityState.setFlag(mob, "sc_used", Integer.MAX_VALUE);
    }
}
