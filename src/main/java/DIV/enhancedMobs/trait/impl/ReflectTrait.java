package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 受けたダメージの一部を攻撃者に跳ね返す。 */
public final class ReflectTrait extends Trait {

    private final double factorPerRank;

    public ReflectTrait(int cost, int weight, int maxRank, int minLevel, double factorPerRank) {
        super("reflect", "REFLECT", cost, weight, maxRank, minLevel);
        this.factorPerRank = factorPerRank;
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker == null) {
            return;
        }
        double reflected = event.getFinalDamage() * factorPerRank * rank;
        if (reflected > 0) {
            attacker.damage(reflected, mob);
        }
    }
}
