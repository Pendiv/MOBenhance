package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 虚空ダメージをキャンセルしてワールドスポーンへテレポートする。 */
public final class WalkingAbyssTrait extends Trait {

    public WalkingAbyssTrait(int cost, int weight, int maxRank, int minLevel) {
        super("walking_abyss", "ABYSS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            mob.teleport(mob.getWorld().getSpawnLocation());
        }
    }
}
