package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/** Overwhelming spacetime power: large health and attack buffs. */
public final class SpacetimeConquerorTrait extends Trait {

    public SpacetimeConquerorTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_conqueror", "STCONQ", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_hp"),
                1.0 + 0.5 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_atk"),
                0.4 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
