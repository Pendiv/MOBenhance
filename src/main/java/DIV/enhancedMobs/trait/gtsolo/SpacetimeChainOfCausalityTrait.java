package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: プレイヤーが mob を殺すたび 3%（固定）で死亡地点へ瞬間移動する。
 * 近隣16mで mob が死ぬたび攻撃力スタックを (1+rank) 個獲得（+6%/個、上限200）し、
 * その 1/3（最低1個）を自身の周囲16mの時空タイプ mob 全員にも分配する。
 */
public final class SpacetimeChainOfCausalityTrait extends Trait {

    private static final NamespacedKey ATK_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_chain_atk");
    private static final String COUNT_KEY = "chain_stacks";
    private static final double TP_SCAN_RADIUS = 64.0;
    private static final double TP_CHANCE = 0.03;       // 固定（rank 非依存）
    private static final double BUFF_RADIUS = 16.0;
    private static final double ATK_PER_STACK = 0.06;   // +6%/個（固定）
    private static final int COUNT_CAP = 200;

    public SpacetimeChainOfCausalityTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_chain_of_causality", "STCHAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    /** {@code MobListener.onDeath} から全死亡で呼ばれる（キル連鎖 TP とスタック蓄積・分配）。 */
    public static void onAnyDeath(LivingEntity dead) {
        Location deathPoint = dead.getLocation();

        // 1) プレイヤーキル → 64m 内の保持者が各 3% で死亡地点へ TP。
        if (dead.getKiller() != null) {
            for (Entity e : dead.getNearbyEntities(TP_SCAN_RADIUS, TP_SCAN_RADIUS, TP_SCAN_RADIUS)) {
                if (!(e instanceof LivingEntity holder) || holder.isDead()
                        || !MobData.of(holder).isProcessed() || rankOf(holder) <= 0) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() < TP_CHANCE) {
                    Mobs.teleport(holder, deathPoint);
                }
            }
        }

        // 2) 任意の死 → 16m 内の保持者がスタック獲得し、1/3 を周囲の時空 mob へ分配。
        for (Entity e : dead.getNearbyEntities(BUFF_RADIUS, BUFF_RADIUS, BUFF_RADIUS)) {
            if (!(e instanceof LivingEntity holder) || holder.isDead()
                    || !MobData.of(holder).isProcessed()) {
                continue;
            }
            int rank = rankOf(holder);
            if (rank <= 0) {
                continue;
            }
            int gain = 1 + rank;  // rank 分を追加殺害とみなす
            addStacks(holder, gain);
            int share = Math.max(1, gain / 3);
            for (Entity allyEntity : holder.getNearbyEntities(BUFF_RADIUS, BUFF_RADIUS, BUFF_RADIUS)) {
                if (allyEntity instanceof LivingEntity ally && !(ally instanceof Player)
                        && ally != dead && MobTags.has(ally, "spacetime")) {
                    addStacks(ally, share);
                }
            }
        }
    }

    private static int rankOf(LivingEntity holder) {
        for (Map.Entry<Trait, Integer> entry : EnhancedMobs.get().traits().read(holder).entrySet()) {
            if (entry.getKey().id().equals("spacetime_chain_of_causality")) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private static void addStacks(LivingEntity mob, int add) {
        int count = Math.min(EntityState.getInt(mob, COUNT_KEY, 0) + add, COUNT_CAP);
        EntityState.setInt(mob, COUNT_KEY, count);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, ATK_KEY,
                count * ATK_PER_STACK, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
