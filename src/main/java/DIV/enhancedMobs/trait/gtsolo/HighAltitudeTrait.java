package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** HPを大幅増加するが、受けるダメージも大幅増加する。高HP・脆い構成。 */
public final class HighAltitudeTrait extends Trait {

    public HighAltitudeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("high_altitude", "HIALT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_high_altitude"),
                3 + rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        event.setDamage(event.getDamage() * (3 + 0.5 * rank));
    }
}
