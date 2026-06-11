package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Color;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Creeper that leaves a lingering poison cloud on death. */
public final class VolatileMixTrait extends Trait {

    public VolatileMixTrait(int cost, int weight, int maxRank, int minLevel) {
        super("volatile_mix", "VOLATILE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        Entity spawned = mob.getWorld().spawnEntity(mob.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        if (!(spawned instanceof AreaEffectCloud cloud)) {
            return;
        }
        cloud.setRadius(2.0f + rank);
        cloud.setRadiusOnUse(0f);
        cloud.setRadiusPerTick(0f);
        cloud.setDuration(100 + 40 * rank);
        cloud.setWaitTime(0);
        cloud.setReapplicationDelay(20);
        cloud.setColor(Color.LIME);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, rank - 1, true, true, true), true);
    }
}
