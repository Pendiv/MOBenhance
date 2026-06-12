package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

/** ゾンビの増援召喚確率を基礎値の2倍にする（原典: MULTIPLY_TOTAL +1.0、rank非依存）。 */
public final class KinCallTrait extends Trait {

    public KinCallTrait(int cost, int weight, int maxRank, int minLevel) {
        super("kin_call", "KIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Zombie;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // MULTIPLY_SCALAR_1 +1.0 = 原典の MULTIPLY_TOTAL「×2」を厳密再現
        Mobs.addModifier(mob, Attribute.SPAWN_REINFORCEMENTS,
                new NamespacedKey(EnhancedMobs.get(), "trait_kin_call"), 1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
