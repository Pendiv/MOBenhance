package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 割れ窓理論 — 100t（5秒）ごとに、半径12内で特性を1つ以上持ちレベルが最も高い Mob を選び、
 * その特性セットから1つ（broken_window 自体は除外）をランダム抽出して
 * 自身と周囲の Mob 最大5体にばら撒く。付与 rank は 1〜参照元rank のランダム
 * （既存 rank がそれ以上なら上書きしない）。
 */
public final class BrokenWindowTrait extends Trait {

    private static final double RADIUS = 12.0;
    private static final int INTERVAL_TICKS = 100;
    private static final int SPREAD_TARGET_LIMIT = 5;

    public BrokenWindowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("broken_window", "BROKEN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "bw_cd")) {
            return;
        }
        EntityState.setFlag(mob, "bw_cd", INTERVAL_TICKS);

        TraitService traits = EnhancedMobs.get().traits();
        List<Mob> neighbors = new ArrayList<>();
        for (Entity entity : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof Mob other && !other.isDead() && MobData.of(other).isProcessed()) {
                neighbors.add(other);
            }
        }

        // 特性を1つ以上持ち、レベルが最も高い個体を参照元とする。
        Mob source = null;
        int highestLevel = -1;
        Map<Trait, Integer> sourceTraits = null;
        for (Mob other : neighbors) {
            Map<Trait, Integer> read = traits.read(other);
            if (read.isEmpty()) {
                continue;
            }
            int level = MobData.of(other).getLevel();
            if (level > highestLevel) {
                highestLevel = level;
                source = other;
                sourceTraits = read;
            }
        }
        if (source == null) {
            return;
        }

        List<Map.Entry<Trait, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<Trait, Integer> entry : sourceTraits.entrySet()) {
            if (!entry.getKey().id().equals(id())) {
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map.Entry<Trait, Integer> picked = candidates.get(random.nextInt(candidates.size()));

        spread(traits, mob, picked, random);
        int count = 0;
        for (Mob other : neighbors) {
            if (count >= SPREAD_TARGET_LIMIT) {
                break;
            }
            spread(traits, other, picked, random);
            count++;
        }
    }

    /** 付与 rank = 1〜参照元rank のランダム。既存 rank 以上なら addTrait 側が無視する。 */
    private void spread(TraitService traits, LivingEntity target,
                        Map.Entry<Trait, Integer> picked, ThreadLocalRandom random) {
        int rank = 1 + random.nextInt(Math.max(1, picked.getValue()));
        traits.addTrait(target, picked.getKey().id(), rank);
    }
}
