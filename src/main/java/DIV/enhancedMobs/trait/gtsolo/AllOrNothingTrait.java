package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 移動速度95%減で事実上不動だが、攻撃は防具を無視してダメージ2倍。 */
public final class AllOrNothingTrait extends Trait {

    public AllOrNothingTrait(int cost, int weight, int maxRank, int minLevel) {
        super("all_or_nothing", "ALLNONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, new NamespacedKey(EnhancedMobs.get(), "trait_all_or_nothing"),
                -0.95, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 2.0);
    }
}
