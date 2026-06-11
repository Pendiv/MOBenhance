package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

/** ゾンビの増援召喚確率を上昇させる。 */
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
        Mobs.addModifier(mob, Attribute.SPAWN_REINFORCEMENTS,
                new NamespacedKey(EnhancedMobs.get(), "trait_kin_call"), 0.2 * rank,
                AttributeModifier.Operation.ADD_NUMBER);
    }
}
