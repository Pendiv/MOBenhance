package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

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
    @SuppressWarnings("deprecation") // DamageModifier API は deprecated だが Paper で現役動作
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        // 素ダメージ2倍（原典: post-armor 量に防具値を足し戻して2倍 = 防御貫通+2x の近似）。
        event.setDamage(event.getDamage() * 2.0);
        // 防具軽減を無効化して防御貫通を直接実現。エンチャント防護(MAGIC)は原典同様貫通しない。
        if (event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        }
    }
}
