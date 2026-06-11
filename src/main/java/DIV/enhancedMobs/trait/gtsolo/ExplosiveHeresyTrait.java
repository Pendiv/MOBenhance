package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** Creeper that takes extra damage from explosions. */
public final class ExplosiveHeresyTrait extends Trait {

    public ExplosiveHeresyTrait(int cost, int weight, int maxRank, int minLevel) {
        super("explosive_heresy", "HERESY", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> event.setDamage(event.getDamage() * 2.5);
            default -> {
            }
        }
    }
}
