package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.base.RangedTrait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

/** 長CDで高速の矢を射ち、命中時にKB耐性を半貫通する強烈なノックバックを与えるスケルトン。 */
public final class CrossbowmanTrait extends RangedTrait {

    public CrossbowmanTrait(int cost, int weight, int maxRank, int minLevel) {
        super("crossbowman", "XBOW", cost, weight, maxRank, minLevel, 300);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected int cooldownTicks(int rank) {
        // 発射速度大幅低下 = 長CD（原典: max(100, 300 - 30N)）
        return Math.max(100, 300 - 30 * rank);
    }

    @Override
    protected Projectile launch(Mob mob, LivingEntity target, int rank) {
        return Mobs.shootArrow(mob, target, 4.0);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity hit)) {
            return;
        }
        // 弩耐性窓: 被弾から30tick以内の再着弾はKB無効（原典準拠の固定窓・延長なし）
        if (!TraitCooldown.ready(hit, "crossbowman", 30, 0, 0, 0)) {
            return;
        }
        Vector dir = projectile.getVelocity();
        double dx = dir.getX();
        double dz = dir.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            return;
        }
        // KB耐性を半分だけ貫通: strength = (5+N) × 0.35 × max(0, 1 - 耐性×0.5)
        AttributeInstance inst = hit.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        double resist = inst != null ? inst.getValue() : 0.0;
        double strength = (5 + rank) * 0.35 * Math.max(0.0, 1.0 - resist * 0.5);
        // 水平は矢の進行方向へ加算、垂直は最低0.42で打ち上げ保証
        Vector v = hit.getVelocity();
        hit.setVelocity(new Vector(v.getX() + dx / len * strength, Math.max(v.getY(), 0.42),
                v.getZ() + dz / len * strength));
    }
}
