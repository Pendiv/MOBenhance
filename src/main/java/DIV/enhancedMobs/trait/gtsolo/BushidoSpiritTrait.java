package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * ボス専用。最大HP +(125+25n)%・攻撃力 +(25+25n)%・防具 +10n を付与して全回復し、
 * 無垢なる戦い（innocence_battle）を同 rank で内部付与する。
 * さらに 2 秒ごとに半径32の他 Mob を消去して「一騎打ち空間」を強制する（ボスは除外）。
 */
public final class BushidoSpiritTrait extends Trait {

    public BushidoSpiritTrait(int cost, int weight, int maxRank, int minLevel) {
        super("bushido_spirit", "BUSHIDO", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        // 原典は L2EntityUtil.isBoss 限定。mod ボスはサーバーに存在しないため vanilla ボスのみ
        return Mobs.isBoss(mob.getType());
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典 MULTIPLY_BASE（= ADD_SCALAR）で HP/ATK、防具は実数加算
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, key("bushido_hp"), 1.25 + 0.25 * rank,
                AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("bushido_atk"), 0.25 + 0.25 * rank,
                AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.ARMOR, key("bushido_arm"), 10.0 * rank,
                AttributeModifier.Operation.ADD_NUMBER);
        mob.setHealth(Mobs.maxHealth(mob));
        // 無垢なる戦いを内部付与（既ランクが同等以上なら維持される）
        EnhancedMobs.get().traits().addTrait(mob, "innocence_battle", rank);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 一騎打ち空間: 2 秒（40t）ごとに周囲32の他 Mob を消去。
        // 原典の MASTER minion 除外は相当概念が無いためボス除外のみ（残差）。
        if (EntityState.hasFlag(mob, "bushido_purge")) {
            return;
        }
        EntityState.setFlag(mob, "bushido_purge", 40);
        for (Entity entity : mob.getNearbyEntities(32, 32, 32)) {
            if (entity instanceof Mob other && !Mobs.isBoss(other.getType())) {
                other.remove();
            }
        }
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
