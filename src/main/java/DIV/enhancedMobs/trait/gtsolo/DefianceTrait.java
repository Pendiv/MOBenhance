package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 被ダメージごとにスタックが増え、攻撃力が上昇する（上限あり）。 */
public final class DefianceTrait extends Trait {

    public DefianceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("defiance", "DEFIANCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityState.addInt(mob, "defiance", 1);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int stacks = Math.min(EntityState.getInt(mob, "defiance", 0), 200 + 100 * rank);
        event.setDamage(event.getDamage() * (1.0 + stacks * 0.001 * (2 + rank)));
    }
}
