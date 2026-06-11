package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;

/** Keeps nearby mobs from dying (heals them); revives itself once at rank 2+. */
public final class SorrowElegyTrait extends Trait {

    public SorrowElegyTrait(int cost, int weight, int maxRank, int minLevel) {
        super("sorrow_elegy", "SORROW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        for (Entity entity : mob.getNearbyEntities(12, 12, 12)) {
            if (entity instanceof Mob other) {
                other.setHealth(Math.min(Mobs.maxHealth(other), other.getHealth() + Mobs.maxHealth(other) * 0.05));
            }
        }
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (rank < 2 || EntityState.hasFlag(mob, "sorrow_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        EntityState.setFlag(mob, "sorrow_used", Integer.MAX_VALUE);
    }
}
