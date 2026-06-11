package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/** 防具無視の追加ダメージを与える矢を射るスケルトン。 */
public final class ArmorShredderTrait extends RangedTrait {

    public ArmorShredderTrait(int cost, int weight, int maxRank, int minLevel) {
        super("armor_shredder", "SHRED", cost, weight, maxRank, minLevel, 50);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 3.0);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof LivingEntity hit) {
            hit.setHealth(Math.max(0, hit.getHealth() - 2.0 * rank));
        }
    }
}
