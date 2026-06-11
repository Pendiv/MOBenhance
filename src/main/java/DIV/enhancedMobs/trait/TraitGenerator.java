package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.config.DimensionConfig;
import DIV.enhancedMobs.config.EntityConfig;
import DIV.enhancedMobs.config.MainConfig;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 重み付きバジェット抽選による特性生成。L2Hostility の {@code TraitGenerator} の簡略移植。
 * レベルがバジェット。各特性のコストは {@code cost * rank}。
 * バジェット切れ・プール枯渇・最大トレイト数到達のいずれかで終了する。
 */
public final class TraitGenerator {

    private final TraitRegistry registry;
    private final MainConfig config;
    private final EntityConfig entityConfig;
    private final DimensionConfig dimensions;

    public TraitGenerator(TraitRegistry registry, MainConfig config, EntityConfig entityConfig,
                          DimensionConfig dimensions) {
        this.registry = registry;
        this.config = config;
        this.entityConfig = entityConfig;
        this.dimensions = dimensions;
    }

    /** 重み付きバジェット抽選。ランクは各トレイトの上限まで自由に上昇する。 */
    public Map<Trait, Integer> generate(LivingEntity mob, int level) {
        double costFactor = config.traitCostFactor;
        boolean free = costFactor < 0.01;
        int maxTrait = free ? Integer.MAX_VALUE : (int) (config.traitMaxCount / costFactor);

        List<Trait> pool = new ArrayList<>();
        for (Trait trait : registry.all()) {
            if (trait.minLevel() <= level
                    && !dimensions.isTraitDisabled(trait.id())
                    && trait.appliesTo(mob)
                    && entityConfig.allows(mob.getType(), trait.id())) {
                pool.add(trait);
            }
        }

        Map<Trait, Integer> result = new LinkedHashMap<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int budget = level;

        while (budget > 0 && !pool.isEmpty()) {
            if (result.size() >= maxTrait) {
                break;
            }
            Trait trait = weightedPick(pool, random);
            pool.remove(trait);

            int cost = trait.getCost(costFactor);
            if (cost > budget) {
                continue;
            }
            int max = Math.min(config.traitGlobalMaxRank, trait.maxRank());
            if (max < 1) {
                continue;
            }
            int rank = free ? max : Math.min(max, 1 + random.nextInt(budget / cost));
            result.put(trait, rank);
            budget -= rank * cost;

            if (random.nextDouble() < config.traitSuppression) {
                break;
            }
        }
        return result;
    }

    private Trait weightedPick(List<Trait> pool, ThreadLocalRandom random) {
        int total = 0;
        for (Trait trait : pool) {
            total += trait.weight();
        }
        int roll = random.nextInt(Math.max(1, total));
        for (Trait trait : pool) {
            roll -= trait.weight();
            if (roll < 0) {
                return trait;
            }
        }
        return pool.get(pool.size() - 1);
    }
}
