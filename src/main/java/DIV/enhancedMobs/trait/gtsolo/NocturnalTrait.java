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
 * 夜間のみ強化される（Diurnal の対特性）。
 * 攻撃力 +(25+25n)% / 移動速度 +25% / 被ダメ軽減 (10+5n)% / 毎秒 rank HP 回復。
 * 夜判定は原典と同じ [13000, 23000)。
 */
public final class NocturnalTrait extends Trait {

    public NocturnalTrait(int cost, int weight, int maxRank, int minLevel) {
        super("nocturnal", "NOCT", cost, weight, maxRank, minLevel);
    }

    /** 原典 NocturnalTrait の夜窓: dayTime % 24000 が [13000, 23000)。 */
    private static boolean isNight(LivingEntity mob) {
        long t = mob.getWorld().getTime() % 24000;
        return t >= 13000 && t < 23000;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (isNight(mob)) {
            // 原典: MULTIPLY_BASE = Bukkit の ADD_SCALAR。
            Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("nocturnal_atk"), 0.25 + 0.25 * rank,
                    AttributeModifier.Operation.ADD_SCALAR);
            Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, key("nocturnal_spd"), 0.25,
                    AttributeModifier.Operation.ADD_SCALAR);
            double max = Mobs.maxHealth(mob);
            if (mob.getHealth() < max) {
                // 原典: 毎秒 rank HP 回復（tick間隔換算）。
                int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
                mob.setHealth(Math.min(max, mob.getHealth() + rank * interval / 20.0));
            }
        } else {
            removeModifier(mob, Attribute.ATTACK_DAMAGE, key("nocturnal_atk"));
            removeModifier(mob, Attribute.MOVEMENT_SPEED, key("nocturnal_spd"));
        }
    }

    /** 原典の被ダメ軽減属性 (10+5n)% のイベント近似。夜のみ有効。 */
    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (isNight(mob)) {
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
