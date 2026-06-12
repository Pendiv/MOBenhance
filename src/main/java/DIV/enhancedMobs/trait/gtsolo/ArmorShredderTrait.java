package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * スケルトン専用。CD = max(100, 600 − 100×rank) tick ごとに半径32の最寄りプレイヤーへ
 * 照準矢（撃滅の矢）を放つ（AI ターゲットと無関係・非交戦時も発射）。
 * 着弾ダメージへ「対象の防具値 × min(1, 0.5 + 0.25×rank)」を足し戻す防御貫通（原典 ANNIHILATION）。
 */
public final class ArmorShredderTrait extends Trait {

    private static final double RADIUS = 32.0;

    public ArmorShredderTrait(int cost, int weight, int maxRank, int minLevel) {
        super("armor_shredder", "SHRED", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "cd_armor_shredder")) {
            return;
        }
        Player target = Mobs.nearestPlayer(mob, RADIUS);
        if (target == null) {
            return; // 原典準拠: 不発時は CD を立てない
        }
        EntityState.setFlag(mob, "cd_armor_shredder", Math.max(100, 600 - 100 * rank));
        Arrow arrow = shootAimedArrow(mob, target, 3.0, 0.5);
        TraitProjectiles.tag(arrow, id(), rank);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        // この特性の矢による着弾ダメージのみ対象（damager = タグ済み矢）
        if (!(event.getDamager() instanceof Projectile projectile)
                || !id().equals(projectile.getPersistentDataContainer()
                        .get(TraitProjectiles.TRAIT, PersistentDataType.STRING))) {
            return;
        }
        AttributeInstance armor = target.getAttribute(Attribute.ARMOR);
        double armorValue = armor != null ? armor.getValue() : 0.0;
        if (armorValue <= 0) {
            return;
        }
        // 防具値 × 貫通率を足し戻す → 防具が厚いほど痛い（rank1 75% / rank2+ 100%）
        double pierce = Math.min(1.0, 0.5 + 0.25 * rank);
        event.setDamage(event.getDamage() + armorValue * pierce);
    }

    /** 原典 SpecialArrow.aimedArrow 相当: 弧補正（水平距離×0.2）+ ばらつき付きの照準矢。 */
    private static Arrow shootAimedArrow(LivingEntity mob, LivingEntity target, double speed, double inaccuracy) {
        Location eye = mob.getEyeLocation();
        double dx = target.getLocation().getX() - mob.getLocation().getX();
        double dy = target.getLocation().getY() + target.getHeight() / 3.0 - eye.getY();
        double dz = target.getLocation().getZ() - mob.getLocation().getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        Vector dir = new Vector(dx, dy + horiz * 0.2, dz);
        if (dir.lengthSquared() < 1e-6) {
            dir = mob.getLocation().getDirection();
        }
        dir.normalize();
        // vanilla shoot() の三角分布ばらつき近似
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double spread = 0.0172275 * inaccuracy;
        dir.add(new Vector((random.nextDouble() - random.nextDouble()) * spread,
                (random.nextDouble() - random.nextDouble()) * spread,
                (random.nextDouble() - random.nextDouble()) * spread));
        Arrow arrow = mob.getWorld().spawn(eye, Arrow.class);
        arrow.setShooter(mob);
        arrow.setVelocity(dir.multiply(speed));
        return arrow;
    }
}
