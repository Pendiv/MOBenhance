package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 死亡時、同地点に同種の新規個体として回帰する（元個体は通常どおり死亡し、ドロップ・経験値も出る）。
 * 復活体は通常スポーン相場のレベルで、60%で本特性そのもの、40%で時空プール13種からの抽選特性を
 * rank=max(1, 元rank) で「1個だけ」持つ（原典 SELF_BIAS=0.6）。
 * 暴走防止に世代カウンタを持ち、5世代で連鎖を打ち切る（原典 MAX_GEN=5）。
 */
public final class SpacetimeEternalReturnTrait extends Trait {

    /** 復活連鎖の上限世代（原典 MAX_GEN、暴走防止）。 */
    private static final int MAX_GEN = 5;
    /** 復活体が永劫回帰そのものを引く確率（原典 SELF_BIAS）。 */
    private static final double SELF_BIAS = 0.6;
    /** 過密抑止: 半径16の同種数がこの数以上なら回帰しない（プラグイン側の安全装置）。 */
    private static final int MAX_NEARBY = 6;

    public SpacetimeEternalReturnTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_eternal_return", "STRETN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        // ボスは自己複製禁止 — 召喚直後の無敵ウィザー増殖等を防ぐ。
        return !Mobs.isBoss(mob.getType());
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int gen = EntityState.getInt(mob, "er_gen", 0);
        if (gen >= MAX_GEN || Mobs.isBoss(mob.getType())) {
            return;
        }
        // 過密抑止（拡散などで群れ全体が本特性を得たときの連鎖湧きを抑える）。
        long sameType = mob.getNearbyEntities(16, 16, 16).stream()
                .filter(e -> e.getType() == mob.getType())
                .count();
        if (sameType >= MAX_NEARBY) {
            return;
        }
        EnhancedMobs plugin = EnhancedMobs.get();
        // 復活体のレベルは通常スポーン相場（原典は L2H の自然スポーン処理に委ねる）。
        int level = plugin.mobBonus().apply(mob.getType(),
                plugin.difficulty().compute(mob.getLocation()));
        String granted = ThreadLocalRandom.current().nextDouble() < SELF_BIAS
                ? id()
                : SpacetimeTraits.randomGrantableId();
        // スポーンイベント前に世代+レベルを書き込み、onSpawn の自動特性抽選を抑止する
        // → 原典どおり「抽選した特性1個だけを持つ個体」になる。位置・向きは死亡地点をコピー。
        Entity created = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType(),
                CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                    if (e instanceof LivingEntity child) {
                        EntityState.setInt(child, "er_gen", gen + 1);
                        MobData.of(child).setLevel(level);
                    }
                });
        if (!(created instanceof LivingEntity child)) {
            return;
        }
        plugin.levelScaler().apply(child, level);
        if (!plugin.traits().addTrait(child, granted, Math.max(1, rank))) {
            // 付与不可（次元無効など）でも頭上表示は付ける（addTrait 成功時は内部で更新される）。
            plugin.traitDisplay().attach(child, level, "");
        }
        child.setHealth(Mobs.maxHealth(child));
        if (plugin.mainConfig().glowEnabled && level >= plugin.mainConfig().glowStrongLevel) {
            child.setGlowing(true);
        }
    }
}
