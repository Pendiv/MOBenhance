package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/** HP20%以下の状態で被弾した瞬間に一度だけ、最近傍プレイヤーとHP割合を入れ替える。 */
public final class LastStandTrait extends Trait {

    public LastStandTrait(int cost, int weight, int maxRank, int minLevel) {
        super("last_stand", "LASTSTAND", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "ls_used")) {
            return;
        }
        // 原典は減算前の現在HPで判定（= 一度20%以下で生き残った次の被弾で発動）
        double mobRatio = Mobs.healthRatio(mob);
        if (mobRatio > 0.2) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 32);
        if (player == null) {
            return;
        }
        double playerRatio = player.getHealth() / Mobs.maxHealth(player);
        // クランプは setHealth の上限例外対策のみ（原典は無加工の比率交換）
        mob.setHealth(Math.min(Mobs.maxHealth(mob), Mobs.maxHealth(mob) * playerRatio));
        player.setHealth(Math.min(Mobs.maxHealth(player), Mobs.maxHealth(player) * mobRatio));
        EntityState.setFlag(mob, "ls_used", Integer.MAX_VALUE);
    }
}
