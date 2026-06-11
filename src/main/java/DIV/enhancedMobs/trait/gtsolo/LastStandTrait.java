package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/** Once, below 20% health, swaps health ratio with the nearest player. */
public final class LastStandTrait extends Trait {

    public LastStandTrait(int cost, int weight, int maxRank, int minLevel) {
        super("last_stand", "LASTSTAND", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "ls_used")) {
            return;
        }
        if ((mob.getHealth() - event.getFinalDamage()) / Mobs.maxHealth(mob) >= 0.2) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 32);
        if (player == null) {
            return;
        }
        double mobRatio = Mobs.healthRatio(mob);
        double playerRatio = player.getHealth() / Mobs.maxHealth(player);
        mob.setHealth(Math.max(1, Mobs.maxHealth(mob) * playerRatio));
        player.setHealth(Math.max(1, Mobs.maxHealth(player) * mobRatio));
        EntityState.setFlag(mob, "ls_used", Integer.MAX_VALUE);
    }
}
