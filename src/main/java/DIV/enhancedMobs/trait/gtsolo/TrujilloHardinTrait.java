package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * 移動速度が base 値を超えた時のみ発動。超過分の速度を base まで引き戻し、
 * 超過率 × (1 + 0.12×rank) を攻撃力の乗算ボーナスへ変換する（定期再計算）。
 */
public final class TrujilloHardinTrait extends Trait {

    public TrujilloHardinTrait(int cost, int weight, int maxRank, int minLevel) {
        super("trujillo_hardin", "TRUJILLO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (speed == null) {
            return;
        }
        // 自分の貼った modifier を外してから素の超過分を再計算する。
        removeModifier(speed, key("trujillo_cap"));
        if (attack != null) {
            removeModifier(attack, key("trujillo_atk"));
        }
        double base = speed.getBaseValue();
        double current = speed.getValue();
        if (base <= 0 || current <= base) {
            return;
        }
        double excess = (current - base) / base;
        // 速度を base まで引き戻す（原典 MULTIPLY_TOTAL = MULTIPLY_SCALAR_1）。
        Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, key("trujillo_cap"),
                base / current - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        // 超過率を攻撃力へ変換（原典 MULTIPLY_BASE = ADD_SCALAR）。
        if (attack != null) {
            Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("trujillo_atk"),
                    excess * (1.0 + 0.12 * rank), AttributeModifier.Operation.ADD_SCALAR);
        }
    }

    private static void removeModifier(AttributeInstance inst, NamespacedKey key) {
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
