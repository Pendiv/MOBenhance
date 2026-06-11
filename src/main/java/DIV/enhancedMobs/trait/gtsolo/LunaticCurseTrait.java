package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Once, slashes the attacking player's health down to 20. */
public final class LunaticCurseTrait extends Trait {

    public LunaticCurseTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lunatic_curse", "LUNATIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player player) || EntityState.hasFlag(mob, "lc_used")) {
            return;
        }
        player.setHealth(Math.min(player.getHealth(), 20.0));
        EntityState.setFlag(mob, "lc_used", Integer.MAX_VALUE);
    }
}
