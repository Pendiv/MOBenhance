package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Survives one lethal hit at 1 HP, then gains brief Resistance V. */
public final class EndureTrait extends Trait {

    public EndureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("endure", "ENDURE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "endure_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(1);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, true, true, true));
        EntityState.setFlag(mob, "endure_used", Integer.MAX_VALUE);
    }
}
