package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

/** 速度4.0の矢を高速連射するスケルトン狙撃手。 */
public final class MagicBulletMarksmanTrait extends RangedTrait {

    public MagicBulletMarksmanTrait(int cost, int weight, int maxRank, int minLevel) {
        super("magic_bullet_marksman", "MARKSMAN", cost, weight, maxRank, minLevel, 30);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 4.0);
    }
}
