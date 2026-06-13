package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 周期的に、特性を持たない自身のクローンを射出する（原典 MonotoneClone）。
 *
 * <p>CD = 36÷(rank+1) 秒（lv1=18s / lv2=12s / lv3=9s）。被弾するたび次回発火が1秒早まる
 * （= 削るほど増える）。発火時は rank 体のクローンを生成し、各クローンは
 * レベル = 親の75〜100%・特性なし・ランダムな水平方向へ射出される。
 *
 * <p>安全装置（原典より厳しい側を維持）: ボス除外、半径16の同種6体上限（残枠まで生成数を丸める）。
 */
public final class MonotoneCloneTrait extends Trait {

    private static final int MAX_NEARBY = 6;
    private static final String NEXT_KEY = "clone_next";

    public MonotoneCloneTrait(int cost, int weight, int maxRank, int minLevel) {
        super("monotone_clone", "CLONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        // ボス（ウィザー/ドラゴン/ウォーデン等）は自己複製禁止 — 止まらない増殖を防ぐため。
        return !Mobs.isBoss(mob.getType());
    }

    /** 原典 CD = 36 ÷ (rank+1) 秒 → tick 換算。 */
    private static int cdTicks(int rank) {
        return (int) (36.0 / (rank + 1) * 20);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        long now = EntityState.gameTime();
        double next = EntityState.getDouble(mob, NEXT_KEY, -1);
        if (next < 0) {
            EntityState.setDouble(mob, NEXT_KEY, now + cdTicks(rank));
            return;
        }
        if (now < next) {
            return;
        }
        EntityState.setDouble(mob, NEXT_KEY, now + cdTicks(rank));
        spawnClones(mob, rank);
    }

    /** 被弾で次回発火を 20 tick（1秒）前倒しする（原典 onHurtByOthers の被弾加速）。 */
    @Override
    public void onAttackedBy(LivingEntity mob, int rank, @Nullable LivingEntity attacker,
                             EntityDamageByEntityEvent event) {
        if (EntityState.getDouble(mob, NEXT_KEY, -1) >= 0) {
            EntityState.addDouble(mob, NEXT_KEY, -20);
        }
    }

    private static void spawnClones(LivingEntity mob, int rank) {
        // 個体数制限: 同種が一定数以上いれば残枠まで生成数を丸める（無限増殖防止の安全装置）。
        long sameType = mob.getNearbyEntities(16, 16, 16).stream()
                .filter(e -> e.getType() == mob.getType())
                .count();
        int count = (int) Math.min(rank, MAX_NEARBY - sameType);
        int parentLevel = MobData.of(mob).getLevel();
        if (count <= 0 || parentLevel < 1) {
            return;
        }
        EnhancedMobs plugin = EnhancedMobs.get();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            // 原典: クローンレベル = 親の 75〜100%。
            int cloneLevel = Math.max(1, (int) (parentLevel * (0.75 + random.nextDouble() * 0.25)));
            Location loc = mob.getLocation();
            loc.setYaw(random.nextFloat() * 360f);
            // スポーンイベント前にレベルを焼き込み、MobListener の自然抽選（特性付与）を抑止する。
            // → クローンは特性を一切持たない（原典準拠 + 交差増殖防止の安全装置）。
            Entity copy = mob.getWorld().spawnEntity(loc, mob.getType(), CreatureSpawnEvent.SpawnReason.CUSTOM,
                    spawned -> {
                        if (spawned instanceof LivingEntity living) {
                            MobData.of(living).setLevel(cloneLevel);
                        }
                    });
            if (!(copy instanceof LivingEntity living)) {
                continue;
            }
            plugin.levelScaler().apply(living, cloneLevel);
            if (plugin.mainConfig().glowEnabled && cloneLevel >= plugin.mainConfig().glowStrongLevel) {
                living.setGlowing(true);
            }
            // 原典: ランダムな水平方向へ射出（cos×0.5, 0.35, sin×0.5）。
            double ang = random.nextDouble() * Math.PI * 2;
            living.setVelocity(new Vector(Math.cos(ang) * 0.5, 0.35, Math.sin(ang) * 0.5));
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (living.isValid()) {
                    plugin.traitDisplay().attach(living, cloneLevel, "");
                }
            });
        }
    }
}
