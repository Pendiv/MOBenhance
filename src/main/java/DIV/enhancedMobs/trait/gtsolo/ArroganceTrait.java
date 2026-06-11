package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 毎ティック高速自己回復するが、プレイヤーに攻撃されると一定時間回復が停止する。 */
public final class ArroganceTrait extends Trait {

    public ArroganceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("arrogance", "ARROG", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!EntityState.hasFlag(mob, "arr_block")) {
            mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + Mobs.maxHealth(mob) * 0.07));
        }
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            EntityState.setFlag(mob, "arr_block", Math.max(20, (7 - rank) * 20));
        }
    }
}
