package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Negates all fire and freeze damage. */
public final class EctothermTrait extends Trait {

    public EctothermTrait(int cost, int weight, int maxRank, int minLevel) {
        super("ectotherm", "ECTO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, FREEZE -> event.setCancelled(true);
            default -> {
            }
        }
    }
}
