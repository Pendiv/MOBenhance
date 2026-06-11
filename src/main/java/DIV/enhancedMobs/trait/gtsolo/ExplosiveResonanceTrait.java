package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 爆発ダメージをキャンセルし、同量のHPを回復する。 */
public final class ExplosiveResonanceTrait extends Trait {

    public ExplosiveResonanceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("explosive_resonance", "RESON", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> {
                event.setCancelled(true);
                mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + event.getDamage()));
            }
            default -> {
            }
        }
    }
}
