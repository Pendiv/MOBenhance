package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Strong damage reduction, but always takes a small chip of max health. */
public final class KongoYashaTrait extends Trait {

    public KongoYashaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("kongo_yasha", "KONGO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            event.setDamage(event.getDamage() * 0.4 + Mobs.maxHealth(mob) * 0.0038);
        }
    }
}
