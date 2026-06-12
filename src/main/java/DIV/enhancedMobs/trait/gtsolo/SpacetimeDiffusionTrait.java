package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 近くに新しく湧いた mob が、確率 (4+best)%（上限50%、best = 16m 内の保持者の最大 rank）で
 * 時空タイプの特性を1つ獲得する（rank = 1〜best の乱数、既ランクが高ければ維持）。
 * 効果はスポーンフック側（{@link #onMobSpawn}）にあり、本体は保持マーカー。
 */
public final class SpacetimeDiffusionTrait extends Trait {

    private static final double RADIUS = 16.0;
    private static final double P_BASE = 0.04;       // (4 + best)%
    private static final double P_PER_RANK = 0.01;
    private static final double P_CAP = 0.5;

    public SpacetimeDiffusionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_diffusion", "STDIFF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    /**
     * {@code MobListener.onSpawn} の初期化後に呼ばれる。16m 内の敷衍保持者の最大 rank に応じて
     * 新規 mob へ時空特性を抽選付与する（チャンクロード復元は CreatureSpawnEvent 対象外 = 原典の
     * loadedFromDisk 除外と同等）。
     */
    public static void onMobSpawn(LivingEntity newMob) {
        int best = 0;
        for (Entity e : newMob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(e instanceof LivingEntity holder) || holder.isDead()
                    || !MobData.of(holder).isProcessed()) {
                continue;
            }
            best = Math.max(best, rankOf(holder));
        }
        if (best <= 0) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double p = Math.min(P_CAP, P_BASE + P_PER_RANK * best);
        if (rnd.nextDouble() >= p) {
            return;
        }
        String id = SpacetimeTraits.randomGrantableId();
        int rank = 1 + rnd.nextInt(best);
        // addTrait は既ランクが高ければ維持し、付与時に initialize + 表示更新まで行う。
        EnhancedMobs.get().traits().addTrait(newMob, id, rank);
    }

    private static int rankOf(LivingEntity holder) {
        for (Map.Entry<Trait, Integer> entry : EnhancedMobs.get().traits().read(holder).entrySet()) {
            if (entry.getKey().id().equals("spacetime_diffusion")) {
                return entry.getValue();
            }
        }
        return 0;
    }
}
