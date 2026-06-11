package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Negates the very first player attack the mob takes. */
public final class FirstMeetingTrait extends Trait {

    public FirstMeetingTrait(int cost, int weight, int maxRank, int minLevel) {
        super("first_meeting", "FIRST", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player) || EntityState.hasFlag(mob, "first_used")) {
            return;
        }
        event.setCancelled(true);
        EntityState.setFlag(mob, "first_used", Integer.MAX_VALUE);
    }
}
