package DIV.enhancedMobs.trait.impl;

import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 致死の一撃を全回復で無効化し続ける「不死」。回数無制限。
 *
 * <p><b>対抗手段（L2 の CURSE プロトコル）</b>: 蘇生は「全回復による死の回避」なので、
 * 回復倍率（attributelib heal_multiplier）が 0 の間は蘇生量が 0 となりそのまま死ぬ。
 * 朽ちた英雄の賛歌・旭の弔いなどの回復封印スキルが唯一の有効打。</p>
 */
public final class UndyingTrait extends Trait {

    /**
     * 蘇生CD（tick）。オーラ等の毎tickダメージで「全回復→即死→全回復」を無限ループ（音スパム）
     * するのを防ぐためだけのガード。通常戦闘で蘇生を妨げる長さではない（満HP分を2回削るには
     * このCDより遥かに時間がかかるため、対プレイヤーでは実質無制限のまま）。
     */
    private static final int REVIVE_CD = 60;

    public UndyingTrait(int cost, int weight, int maxRank, int minLevel) {
        super("undying", "UNDYING", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // 無敵貫通（/kill・奈落）と継続的な環境ダメージは対象外。直近に蘇生していたら再発火しない。
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID
                || Mobs.isEnvironmentalDoT(cause) || EntityState.hasFlag(mob, "revive_cd")) {
            return;
        }
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        // 蘇生 = 全回復。回復倍率0（封印・呪い）なら蘇生量0でそのまま死ぬ。
        double revive = Mobs.maxHealth(mob) * Attributes.get(mob, StandardAttributes.HEAL_MULTIPLIER);
        if (revive <= 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), revive));
        EntityState.setFlag(mob, "revive_cd", REVIVE_CD);
        Mobs.playRevivalEffect(mob);
    }
}
