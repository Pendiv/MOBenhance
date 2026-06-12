package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * スケルトンが雷矢を放つ。命中対象の足元に落雷し、対象の攻撃力に比例した追加ダメージを与える。
 * アグロ不要で最寄りプレイヤー（半径32）を狙い、CDは固定20秒（原典準拠）。
 */
public final class LightningUserTrait extends Trait {

    private static final int COOLDOWN_TICKS = 400;
    private static final double SEARCH_RADIUS = 32.0;

    public LightningUserTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lightning_user", "LIGHT", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        String cooldown = "cd_" + id();
        if (EntityState.hasFlag(mob, cooldown)) {
            return;
        }
        Player target = Mobs.nearestPlayer(mob, SEARCH_RADIUS);
        if (target == null) {
            return;
        }
        EntityState.setFlag(mob, cooldown, COOLDOWN_TICKS);
        Arrow arrow = Mobs.shootArrow(mob, target, 3.0);
        TraitProjectiles.tag(arrow, id(), rank);
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        // エンティティ命中時のみ発雷（ブロック着弾では何も起きない。矢自体のダメージは通常通り）
        if (!(event.getHitEntity() instanceof LivingEntity hit)) {
            return;
        }
        hit.getWorld().strikeLightning(hit.getLocation());
        // 対象の攻撃力 × (0.3 + 0.2×rank) の追加ダメージ（攻撃力が高い相手ほど痛い）
        AttributeInstance attack = hit.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        double bonus = attack.getValue() * (0.3 + 0.2 * rank);
        if (bonus > 0) {
            if (shooter != null) {
                hit.damage(bonus, shooter);
            } else {
                hit.damage(bonus);
            }
        }
    }
}
