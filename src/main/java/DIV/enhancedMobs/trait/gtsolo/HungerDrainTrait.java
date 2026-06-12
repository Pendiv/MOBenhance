package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃時にプレイヤーの消耗度を 2.0×ランク 加算する（食料 0.5×ランク 相当）。 */
public final class HungerDrainTrait extends Trait {

    public HungerDrainTrait(int cost, int weight, int maxRank, int minLevel) {
        super("hunger_drain", "HUNGER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target instanceof Player player) {
            // 原典は addExhaustion(2.0 × lv)。隠し満腹度から先に削れる vanilla 仕様に乗る
            player.setExhaustion(player.getExhaustion() + 2.0f * rank);
        }
    }
}
