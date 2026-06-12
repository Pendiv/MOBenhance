package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.DamageLib;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * 1 辺 (4+N) ブロックの立方体オーラ。20t ごとに範囲内の自分以外の全 LivingEntity
 * （モブ含む）へ 3 + 攻撃力×0.25N の魔法ダメージ（防具素通り・壁貫通）を与える。
 * 立方体 12 辺を SOUL_FIRE_FLAME で描画し、プレイヤーが境界を視認して回避できる。
 */
public final class DamageAuraTrait extends Trait {

    /** 各辺のパーティクルサンプル数（原典と同じ 6 点 = 0..EDGE_SAMPLES）。 */
    private static final int EDGE_SAMPLES = 5;
    private static final int DAMAGE_INTERVAL = 20;

    public DamageAuraTrait(int cost, int weight, int maxRank, int minLevel) {
        super("damage_aura", "DMGAURA", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double half = (4.0 + rank) / 2.0;
        emitCubeOutline(mob, half);
        if (EntityState.hasFlag(mob, "damage_aura_cd")) {
            return;
        }
        EntityState.setFlag(mob, "damage_aura_cd", DAMAGE_INTERVAL);
        AttributeInstance atkInst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        double atk = atkInst != null ? atkInst.getValue() : 0.0;
        double amount = 3.0 + atk * 0.25 * rank;
        for (Entity entity : mob.getNearbyEntities(half, half, half)) {
            if (entity instanceof LivingEntity target && !target.isDead()) {
                // 原典どおりの魔法ダメージ（attributelib:magic = 防具素通り・魔法軽減%有効・攻撃者帰属）
                DamageLib.magic(mob, target, amount);
            }
        }
    }

    /** 立方体 12 辺に沿って SOUL_FIRE_FLAME を散布（原典 10t → trait tick 周期に粗化）。 */
    private static void emitCubeOutline(LivingEntity mob, double h) {
        World world = mob.getWorld();
        Location c = mob.getLocation();
        for (int i = 0; i <= EDGE_SAMPLES; i++) {
            double p = (i / (double) EDGE_SAMPLES) * 2.0 * h - h;
            // X 方向 4 辺: (p, ±h, ±h)
            point(world, c, p, h, h);
            point(world, c, p, h, -h);
            point(world, c, p, -h, h);
            point(world, c, p, -h, -h);
            // Y 方向 4 辺: (±h, p, ±h)
            point(world, c, h, p, h);
            point(world, c, h, p, -h);
            point(world, c, -h, p, h);
            point(world, c, -h, p, -h);
            // Z 方向 4 辺: (±h, ±h, p)
            point(world, c, h, h, p);
            point(world, c, h, -h, p);
            point(world, c, -h, h, p);
            point(world, c, -h, -h, p);
        }
    }

    private static void point(World world, Location center, double dx, double dy, double dz) {
        world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                center.getX() + dx, center.getY() + dy, center.getZ() + dz, 1, 0, 0, 0, 0);
    }
}
