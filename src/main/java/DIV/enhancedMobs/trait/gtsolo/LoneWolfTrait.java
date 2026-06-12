package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/** 周囲16ブロック以内に他のモブがいない間、攻撃力と最大HPが +25%×rank 上昇する。 */
public final class LoneWolfTrait extends Trait {

    private static final double RADIUS = 16.0;
    private static final double BUFF_PER_RANK = 0.25;

    public LoneWolfTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lone_wolf", "LONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        boolean alone = true;
        for (Entity entity : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof Mob) {
                alone = false;
                break;
            }
        }
        double buff = BUFF_PER_RANK * rank;
        // 攻撃力: ADD_SCALAR = 原典の MULTIPLY_BASE と同演算
        NamespacedKey attackKey = new NamespacedKey(EnhancedMobs.get(), "trait_lone_wolf_attack");
        AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            if (alone) {
                Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, attackKey, buff,
                        AttributeModifier.Operation.ADD_SCALAR);
            } else {
                removeModifier(attack, attackKey);
            }
        }
        // 最大HP: HP割合を保存して付け外し（往復で無料回復・即死級減少が起きない）
        NamespacedKey healthKey = new NamespacedKey(EnhancedMobs.get(), "trait_lone_wolf_health");
        AttributeInstance health = mob.getAttribute(Attribute.MAX_HEALTH);
        if (health == null) {
            return;
        }
        boolean buffed = health.getModifiers().stream().anyMatch(m -> healthKey.equals(m.getKey()));
        if (alone == buffed) {
            return;
        }
        double ratio = Mobs.healthRatio(mob);
        if (alone) {
            health.addModifier(new AttributeModifier(healthKey, buff, AttributeModifier.Operation.ADD_SCALAR));
        } else {
            removeModifier(health, healthKey);
        }
        mob.setHealth(Math.min(health.getValue(), ratio * health.getValue()));
    }

    private static void removeModifier(AttributeInstance inst, NamespacedKey key) {
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }
}
