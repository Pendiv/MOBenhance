package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

/** スケルトンが目標に向かって精密な矢を射る。 */
public final class HomingShotTrait extends RangedTrait {

    public HomingShotTrait(int cost, int weight, int maxRank, int minLevel) {
        super("homing_shot", "HOMING", cost, weight, maxRank, minLevel, 40);
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
