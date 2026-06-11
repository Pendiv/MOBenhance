package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/** Boss-like: large health, armor and attack buffs. */
public final class BushidoSpiritTrait extends Trait {

    public BushidoSpiritTrait(int cost, int weight, int maxRank, int minLevel) {
        super("bushido_spirit", "BUSHIDO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, key("bushido_hp"), 1.0 + 0.5 * rank,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("bushido_atk"), 0.5 + 0.25 * rank,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ARMOR, key("bushido_arm"), 8.0 * rank,
                AttributeModifier.Operation.ADD_NUMBER);
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
