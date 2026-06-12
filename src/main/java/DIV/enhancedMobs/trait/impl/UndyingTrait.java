package DIV.enhancedMobs.trait.impl;

import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * 致死の一撃を1度だけ無効化し、全回復する。
 *
 * <p><b>対抗手段（L2 原典のプロトコル）</b>: 蘇生は「全回復による死の回避」なので、
 * 回復倍率（attributelib heal_multiplier）が 0 以下の間は蘇生量が 0 となり、
 * そのまま死亡する。朽ちた英雄の賛歌・旭の弔いなどの回復封印スキルが有効打になる。</p>
 */
public final class UndyingTrait extends Trait {

    private final NamespacedKey usedKey;

    public UndyingTrait(int cost, int weight, int maxRank, int minLevel) {
        super("undying", "UNDYING", cost, weight, maxRank, minLevel);
        this.usedKey = new NamespacedKey(EnhancedMobs.get(), "undying_used");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (mob.getPersistentDataContainer().has(usedKey, PersistentDataType.BYTE)) {
            return;
        }
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        // 蘇生 = 全回復。回復倍率 0（呪い・回復封印）なら蘇生できず、そのまま死亡する。
        double revive = Mobs.maxHealth(mob) * Attributes.get(mob, StandardAttributes.HEAL_MULTIPLIER);
        if (revive <= 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), revive));
        mob.getPersistentDataContainer().set(usedKey, PersistentDataType.BYTE, (byte) 1);
    }
}
