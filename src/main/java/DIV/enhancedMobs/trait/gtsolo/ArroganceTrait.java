package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 毎秒 maxHP×7% を自己回復するが、プレイヤーに攻撃されると一定時間あらゆる回復が停止する。 */
public final class ArroganceTrait extends Trait {

    public ArroganceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("arrogance", "ARROG", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "arr_block")) {
            return;
        }
        // 原典: 20tickごとに maxHP×7% 回復（rank 非依存）。tick間隔コンフィグに比例配分し毎秒7%を維持。
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        double heal = Mobs.maxHealth(mob) * 0.07 * interval / 20.0;
        mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + heal));
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            // 停止時間 = (7−rank)秒、下限1秒（高 rank ほど早く回復再開）。
            int block = Math.max(20, (7 - rank) * 20);
            EntityState.setFlag(mob, "arr_block", block);
            // 停止中はポーション・他特性等の外部回復も遮断（原典の LivingHealEvent cancel 相当）。
            HealMultiplier.applyCurse(mob, 0.0, block);
        }
    }
}
