package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/** HPを大幅増加するが、受けるダメージも大幅増加する。高HP・脆い構成。 */
public final class HighAltitudeTrait extends Trait {

    public HighAltitudeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("high_altitude", "HIALT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_high_altitude"),
                3 + rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        // 被ダメ増加は attributelib の標準属性で常時適用（旧 onAttacked 乗算の置き換え）。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.DAMAGE_TAKEN, Operation.MULTIPLY, 3 + 0.5 * rank);
    }
}
