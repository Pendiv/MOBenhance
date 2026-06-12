package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 初めてプレイヤーに攻撃を当てた時に限り、そのプレイヤーのHPを20に切り下げる（1回限り発動）。 */
public final class LunaticCurseTrait extends Trait {

    public LunaticCurseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lunatic_curse", "LUNATIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player) || EntityState.hasFlag(mob, "lc_used")) {
            return;
        }
        // 原典同様、HP20以下の相手に当てても発動済み扱い。maxHealth は触らない。
        EntityState.setFlag(mob, "lc_used", Integer.MAX_VALUE);
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
        }
    }
}
