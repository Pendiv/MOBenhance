package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 被ダメージごとにスタックが増え、攻撃力が上昇する（上限あり）。 */
public final class DefianceTrait extends Trait {

    public DefianceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("defiance", "DEFIANCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (event.getDamage() <= 0) {
            return;
        }
        // 1スタック = 0.1×(2+N)%、上限 200+100N スタック。
        // 原典どおり ATTACK_DAMAGE 属性へ MULTIPLY_BASE（Bukkit の ADD_SCALAR）で反映する
        int stacks = Math.min(EntityState.addInt(mob, "defiance", 1), 200 + 100 * rank);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_defiance_atk"),
                stacks * 0.001 * (2 + rank), AttributeModifier.Operation.ADD_SCALAR);
    }
}
