package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/** On death, summons a stronger same-type mob. */
public final class SummoningRitualTrait extends Trait {

    public SummoningRitualTrait(int cost, int weight, int maxRank, int minLevel) {
        super("summoning_ritual", "RITUAL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
        if (copy instanceof LivingEntity living) {
            EnhancedMobs.get().initializeMob(living, MobData.of(mob).getLevel() + 5 * rank);
        }
    }
}
