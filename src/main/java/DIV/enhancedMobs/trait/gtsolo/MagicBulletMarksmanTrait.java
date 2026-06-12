package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 魔弾の射手。スケルトン専用のボス級射手（原典 MagicBulletMarksmanTrait）。
 * <ul>
 *   <li>付与時: 攻撃力・最大HP +(10+5N)% + 全回復。被ダメージは (10+5N)% 軽減
 *       （原典 L2DT REDUCTION 属性の onAttacked 近似）</li>
 *   <li>半径48の最寄りプレイヤー（アグロ不要）へ CD 40tick で照準矢（初速3.0・ばらつき1.0）</li>
 *   <li>12ブロック以内に接近されると透明化 + 移動速度 +50%（離れると解除）</li>
 *   <li>特殊矢: 発射ごとに rank×7% + 累積ボーナスの確率で6種プール
 *       {爆裂/雷/鈍足/撃滅/飛墜/飛燕} からランダム1種を初期性能で付与。
 *       通常矢を撃つと +4%、被弾するたび +3% 累積（rank 非参照）。特殊矢を撃つと累積は 0 に戻る</li>
 * </ul>
 */
public final class MagicBulletMarksmanTrait extends Trait {

    private static final double RADIUS = 48.0;
    private static final double CLOSE_DIST = 12.0;
    /** 発射間隔（原典 FIRE_CD = 40tick = 2秒に1射）。 */
    private static final int FIRE_CD = 40;
    /** 特殊矢確率の rank 係数（rank × 7%）。 */
    private static final double BASE_PER_RANK = 0.07;
    /** 通常矢を撃つたびの累積 +4%。 */
    private static final double MISS_GAIN = 0.04;
    /** 被弾するたびの累積 +3%。 */
    private static final double HIT_GAIN = 0.03;
    /** 累積ボーナスの保存キー（EntityState）。 */
    private static final String BONUS = "mbm_bonus";
    /** 飛墜の矢の飛行無効時間（原典 perf 200 = 10秒）。 */
    private static final int PLUMMET_TICKS = 200;

    /** 特殊矢プール（原典 ArrowBehaviors の6種・性能は全て初期値固定）。 */
    private enum BulletType { EXPLOSIVE, LIGHTNING, SLOWNESS, ANNIHILATION, PLUMMET, SWALLOW }

    private static final BulletType[] POOL = BulletType.values();

    public MagicBulletMarksmanTrait(int cost, int weight, int maxRank, int minLevel) {
        super("magic_bullet_marksman", "MARKSMAN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    /** 原典 postInit: 攻撃力・最大HP +(10+5N)%（MULTIPLY_BASE = ADD_SCALAR）+ 全回復。 */
    @Override
    public void initialize(LivingEntity mob, int rank) {
        double pct = 0.10 + 0.05 * rank;
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("mbm_atk"), pct,
                AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, key("mbm_hp"), pct,
                AttributeModifier.Operation.ADD_SCALAR);
        mob.setHealth(Mobs.maxHealth(mob));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        Player near = Mobs.nearestPlayer(mob, RADIUS);

        // 接近されると透明化 + 移動速度上昇（離れると解除）。原典 MULTIPLY_BASE 0.5 = ADD_SCALAR
        boolean close = near != null
                && near.getLocation().distanceSquared(mob.getLocation()) <= CLOSE_DIST * CLOSE_DIST;
        if (close) {
            int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    Math.max(40, interval * 2), 0, false, false));
            Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, key("mbm_spd"), 0.5,
                    AttributeModifier.Operation.ADD_SCALAR);
        } else {
            mob.removePotionEffect(PotionEffectType.INVISIBILITY);
            removeModifier(mob, Attribute.MOVEMENT_SPEED, key("mbm_spd"));
        }

        if (near == null) {
            return;
        }
        String cooldown = "cd_" + id();
        if (EntityState.hasFlag(mob, cooldown)) {
            return;
        }
        EntityState.setFlag(mob, cooldown, FIRE_CD);

        Arrow arrow = shootAimedArrow(mob, near, 3.0, 1.0);
        TraitProjectiles.tag(arrow, id(), rank);
        double prob = rank * BASE_PER_RANK + EntityState.getDouble(mob, BONUS, 0);
        if (ThreadLocalRandom.current().nextDouble() < prob) {
            // 特殊矢: 種類をサブタグで矢に載せ、累積を失効させる
            BulletType type = POOL[ThreadLocalRandom.current().nextInt(POOL.length)];
            arrow.getPersistentDataContainer().set(typeKey(), PersistentDataType.STRING, type.name());
            EntityState.setDouble(mob, BONUS, 0);
        } else {
            // 通常矢 → 確率累積 +4%
            EntityState.addDouble(mob, BONUS, MISS_GAIN);
        }
    }

    /** 原典 REDUCTION 属性の近似: 全被ダメージを (10+5N)% 軽減。 */
    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        event.setDamage(event.getDamage() * (1 - (0.10 + 0.05 * rank)));
    }

    /** 原典 onHurtByOthers: 被弾するたび特殊矢確率 +3% 累積（rank 非参照）。 */
    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker,
                             EntityDamageByEntityEvent event) {
        EntityState.addDouble(mob, BONUS, HIT_GAIN);
    }

    /** 撃滅の矢: ダメージへ対象の防具値 ×100% を加算（防御貫通の近似。原典 ANNIHILATION onHurt と同型）。 */
    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)
                || !BulletType.ANNIHILATION.name().equals(
                        arrow.getPersistentDataContainer().get(typeKey(), PersistentDataType.STRING))) {
            return;
        }
        AttributeInstance armor = target.getAttribute(Attribute.ARMOR);
        if (armor != null && armor.getValue() > 0) {
            event.setDamage(event.getDamage() + armor.getValue());
        }
    }

    @Override
    public void onProjectileHit(LivingEntity shooter, int rank, Projectile projectile, ProjectileHitEvent event) {
        String name = projectile.getPersistentDataContainer().get(typeKey(), PersistentDataType.STRING);
        if (name == null) {
            return; // 通常矢
        }
        LivingEntity hit = event.getHitEntity() instanceof LivingEntity living ? living : null;
        switch (BulletType.valueOf(name)) {
            case EXPLOSIVE -> {
                // 威力5・延焼なし・ブロック破壊なし。壁・対象どちらの着弾でも起爆（射手に帰属）
                Location loc = projectile.getLocation();
                loc.getWorld().createExplosion(shooter, loc, 5.0f, false, false);
                if (hit == null) {
                    projectile.remove(); // 壁着弾の残留矢を除去
                }
            }
            case LIGHTNING -> {
                if (hit == null) {
                    return;
                }
                hit.getWorld().strikeLightning(hit.getLocation());
                // 対象の攻撃力 ×100% の追加ダメージ（原典 perf 1.0）
                AttributeInstance attack = hit.getAttribute(Attribute.ATTACK_DAMAGE);
                if (attack != null && attack.getValue() > 0) {
                    if (shooter != null) {
                        hit.damage(attack.getValue(), shooter);
                    } else {
                        hit.damage(attack.getValue());
                    }
                }
            }
            case SLOWNESS -> {
                // 鈍足I 10秒（原典 perf 1.0 / 200tick）
                if (hit != null) {
                    hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0));
                }
            }
            case SWALLOW -> {
                if (hit == null) {
                    return;
                }
                // 浮遊V 3秒 + 上向き打ち上げ vy ≥ 0.8（原典 perf 5.0）
                hit.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 4));
                Vector v = hit.getVelocity();
                hit.setVelocity(new Vector(v.getX(), Math.max(v.getY(), 0.8), v.getZ()));
            }
            case PLUMMET -> {
                if (hit instanceof Player player) {
                    suppressFlight(player);
                }
            }
            case ANNIHILATION -> {
                // onHurtTarget 側でダメージ加算済み
            }
        }
    }

    /**
     * 飛墜の矢: 10秒間エリトラ滑空を毎 tick 強制解除する
     * （クリエイティブ飛行・MOD 飛行は触らない安全側の近似。再命中で時間延長）。
     */
    private static void suppressFlight(Player player) {
        EntityState.setFlag(player, "mbm_plummet", PLUMMET_TICKS);
        if (FastTick.isRegistered(player, "mbm_plummet")) {
            return;
        }
        FastTick.register(player, "mbm_plummet", () -> {
            if (!player.isValid() || !EntityState.hasFlag(player, "mbm_plummet")) {
                return false;
            }
            if (player.isGliding()) {
                player.setGliding(false);
            }
            return true;
        });
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

    private static void removeModifier(LivingEntity mob, Attribute attribute, NamespacedKey key) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }

    /** 矢に載せる特殊矢種別のサブタグ（TraitProjectiles のタグと併用）。 */
    private static NamespacedKey typeKey() {
        return new NamespacedKey(EnhancedMobs.get(), "mbm_arrow_type");
    }
}
