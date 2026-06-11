package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 自身のHPを1.5倍・攻撃を大幅減にした代わりに、周囲のモブに力を付与するサポーター。 */
public final class HarmoniousMarchTrait extends AuraTrait {

    public HarmoniousMarchTrait(int cost, int weight, int maxRank, int minLevel) {
        super("harmonious_march", "MARCH", cost, weight, maxRank, minLevel, 12.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_march_hp"),
                0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(EnhancedMobs.get(), "trait_march_atk"),
                -(0.36 + 0.18 * rank), AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
    }
}
