package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Counterattacks whoever hits the mob. */
public final class StrikeTrait extends Trait {

    private final double damagePerRank;

    public StrikeTrait(int cost, int weight, int maxRank, int minLevel, double damagePerRank) {
        super("strike", "STRIKE", cost, weight, maxRank, minLevel);
        this.damagePerRank = damagePerRank;
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker == null) {
            return;
        }
        attacker.damage(damagePerRank * rank, mob);
    }
}
