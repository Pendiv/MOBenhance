package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 昼間のみ強化される（Nocturnal の対特性）。
 * 攻撃力 +(25+25n)% / 移動速度 +25% / 被ダメ軽減 (10+5n)% / 毎秒 rank HP 回復。
 * 昼判定は原典と同じ [0,13000)∪[23000,24000)。
 */
public final class DiurnalTrait extends Trait {

    public DiurnalTrait(int cost, int weight, int maxRank, int minLevel) {
        super("diurnal", "DIURNAL", cost, weight, maxRank, minLevel);
    }

    /** 原典 DiurnalTrait の昼窓: dayTime % 24000 が [0,13000) または [23000,24000)。 */
    private static boolean isDay(LivingEntity mob) {
        long t = mob.getWorld().getTime() % 24000;
        return t < 13000 || t >= 23000;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (isDay(mob)) {
            Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("diurnal_atk"), 0.25 + 0.25 * rank,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, key("diurnal_spd"), 0.25,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            double max = Mobs.maxHealth(mob);
            if (mob.getHealth() < max) {
                mob.setHealth(Math.min(max, mob.getHealth() + rank)); // 原典: 毎秒 rank HP 回復
            }
        } else {
            removeModifier(mob, Attribute.ATTACK_DAMAGE, key("diurnal_atk"));
            removeModifier(mob, Attribute.MOVEMENT_SPEED, key("diurnal_spd"));
        }
    }

    /** 原典の被ダメ軽減属性 (10+5n)% のイベント近似。昼のみ有効。 */
    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (isDay(mob)) {
            event.setDamage(event.getDamage() * (1 - (0.10 + 0.05 * rank)));
        }
    }

    private static void removeModifier(LivingEntity mob, Attribute attribute, NamespacedKey key) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
