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
 * 半径 4・厚み 1.5 の円盤オーラ。2 秒（40t）ごとに範囲内の自分以外の全 LivingEntity へ
 * 攻撃力×(0.10+0.30N) の魔法ダメージ（防具素通り・壁貫通）+ 2 秒着火を与える。
 * 円周 24 点を FLAME で描画する。
 */
public final class DeathAuraTrait extends Trait {

    private static final double RADIUS = 4.0;
    private static final int DAMAGE_INTERVAL = 40;
    private static final int RING_SAMPLES = 24;

    public DeathAuraTrait(int cost, int weight, int maxRank, int minLevel) {
        super("death_aura", "DTHAURA", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        emitRing(mob);
        if (EntityState.hasFlag(mob, "death_aura_cd")) {
            return;
        }
        EntityState.setFlag(mob, "death_aura_cd", DAMAGE_INTERVAL);
        AttributeInstance atkInst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        double atk = atkInst != null ? atkInst.getValue() : 0.0;
        double damage = atk * (0.10 + 0.30 * rank);
        if (damage <= 0) {
            return;
        }
        Location center = mob.getLocation();
        for (Entity entity : mob.getNearbyEntities(RADIUS, 1.0, RADIUS)) {
            if (!(entity instanceof LivingEntity target) || target.isDead()) {
                continue;
            }
            // 水平距離による円判定（原典は半径 4 の円柱）
            double dx = target.getLocation().getX() - center.getX();
            double dz = target.getLocation().getZ() - center.getZ();
            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }
            // 原典どおりの魔法ダメージ（attributelib:magic = 防具素通り・魔法軽減%有効・攻撃者帰属）
            DamageLib.magic(mob, target, damage);
            target.setFireTicks(Math.max(target.getFireTicks(), 40));
        }
    }

    /** 半径 4 の円周 24 点に FLAME を散布（原典 5t → trait tick 周期に粗化）。 */
    private static void emitRing(LivingEntity mob) {
        World world = mob.getWorld();
        Location c = mob.getLocation();
        for (int i = 0; i < RING_SAMPLES; i++) {
            double a = (i / (double) RING_SAMPLES) * Math.PI * 2;
            world.spawnParticle(Particle.FLAME,
                    c.getX() + Math.cos(a) * RADIUS, c.getY() + 0.1, c.getZ() + Math.sin(a) * RADIUS,
                    1, 0, 0, 0, 0);
        }
    }
}
