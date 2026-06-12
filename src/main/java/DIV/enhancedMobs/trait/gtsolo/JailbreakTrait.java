package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitService;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ジェイルブレイク — 自分を2つのランダム特性に変換するガチャ特性。
 * 付与時に「cost が rank×100 ±25% かつ weight が 80 ±25%」に収まる登録特性プールから
 * 2つを rank1 で取得し、自身は失効する（表示からも消える）。
 */
public final class JailbreakTrait extends Trait {

    public JailbreakTrait(int cost, int weight, int maxRank, int minLevel) {
        super("jailbreak", "JAIL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 再入ガード（変身引き継ぎ・再付与での二重リロール防止）。
        if (EntityState.getInt(mob, "jailbreak_done", 0) != 0) {
            return;
        }
        EntityState.setInt(mob, "jailbreak_done", 1);
        // generateAndApply の serialize 後に呼ばれるため、書き戻し競合を避けて1tick遅延で変換する。
        EnhancedMobs plugin = EnhancedMobs.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (mob.isValid() && !mob.isDead()) {
                reroll(mob, rank);
            }
        });
    }

    /** プールから2つ抽選して付与し、自身を剥奪する。 */
    private void reroll(LivingEntity mob, int rank) {
        TraitService traits = EnhancedMobs.get().traits();
        double centerCost = rank * (double) cost();
        double costMin = centerCost * 0.75;
        double costMax = centerCost * 1.25;
        double weightMin = weight() * 0.75;
        double weightMax = weight() * 1.25;

        List<Trait> pool = new ArrayList<>();
        for (Trait trait : traits.registry().all()) {
            if (trait == this) {
                continue;
            }
            if (trait.cost() >= costMin && trait.cost() <= costMax
                    && trait.weight() >= weightMin && trait.weight() <= weightMax) {
                pool.add(trait);
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 2 && !pool.isEmpty(); i++) {
            Trait picked = pool.remove(random.nextInt(pool.size()));
            // 原典は適性外でも付与する（no-op 特性化）が、移植側は addTrait の適性チェックに委ねる。
            traits.addTrait(mob, picked.id(), 1);
        }
        traits.stripTrait(mob, id());
    }
}
