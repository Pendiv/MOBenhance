package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;

/** 再定義 GROWTH: 最大HPを +20% + ランク×10% 増加させる。スライム系専用。 */
public final class GrowthTrait extends Trait {

    private final NamespacedKey key;

    public GrowthTrait(int cost, int weight, int maxRank, int minLevel) {
        super("growth", "GROW", cost, weight, maxRank, minLevel);
        this.key = new NamespacedKey(EnhancedMobs.get(), "trait_growth");
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Slime; // MagmaCube は Slime のサブクラスなので含まれる
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.getModifiers().stream()
                .filter(m -> key.equals(m.getKey()))
                .toList()
                .forEach(maxHealth::removeModifier);
        double amount = 0.2 + 0.1 * rank;
        maxHealth.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
