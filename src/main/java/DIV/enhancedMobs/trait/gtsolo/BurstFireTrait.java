package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

/** Skeleton fires rapid bursts of arrows. */
public final class BurstFireTrait extends RangedTrait {

    public BurstFireTrait(int cost, int weight, int maxRank, int minLevel) {
        super("burst_fire", "BURST", cost, weight, maxRank, minLevel, 10);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 3.0);
    }
}
