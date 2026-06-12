package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 時空族: 近隣16mで時空タイプ mob が死ぬたびに「骨」を1つ拾い（上限40）、
 * 攻撃力 +5×rank%/個・最大HP +10%/個 を累積し、拾うたび最大HPの10%を回復する。
 * 最大HP更新時は HP 割合を保存する（原典 setMaxHealthMultPreservingRatio 相当）。
 */
public final class SpacetimeBonePickerTrait extends Trait {

    private static final NamespacedKey ATK_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_bone_picker_atk");
    private static final NamespacedKey HP_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_bone_picker_hp");
    private static final String COUNT_KEY = "bone_stacks";
    private static final double RADIUS = 16.0;
    private static final double ATK_PER_STACK_PER_RANK = 0.05;  // +5×rank%/個
    private static final double HP_PER_STACK = 0.10;            // 最大HP +10%/個
    private static final double HEAL_PCT = 0.10;                // 拾うたび最大HP 10% 回復
    private static final int COUNT_CAP = 40;

    public SpacetimeBonePickerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_bone_picker", "STBONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    /** {@code MobListener.onDeath} から全死亡で呼ばれる。死者が時空 mob なら近隣の保持者が骨を拾う。 */
    public static void onAnyDeath(LivingEntity dead) {
        if (!MobTags.has(dead, "spacetime")) {
            return;
        }
        for (Entity e : dead.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(e instanceof LivingEntity holder) || holder.isDead()
                    || !MobData.of(holder).isProcessed()) {
                continue;
            }
            int rank = rankOf(holder);
            if (rank > 0) {
                applyGain(holder, rank);
            }
        }
    }

    private static int rankOf(LivingEntity holder) {
        for (Map.Entry<Trait, Integer> entry : EnhancedMobs.get().traits().read(holder).entrySet()) {
            if (entry.getKey().id().equals("spacetime_bone_picker")) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private static void applyGain(LivingEntity mob, int rank) {
        int count = Math.min(EntityState.getInt(mob, COUNT_KEY, 0) + 1, COUNT_CAP);
        EntityState.setInt(mob, COUNT_KEY, count);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, ATK_KEY,
                count * ATK_PER_STACK_PER_RANK * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        // 最大HPは割合保存で更新（バーが激変しない）。
        double ratio = Mobs.healthRatio(mob);
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, HP_KEY,
                count * HP_PER_STACK, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), Math.max(1.0, ratio * Mobs.maxHealth(mob))));
        // 拾うたび最大HPの10%回復（回復倍率を尊重）。
        double heal = Mobs.maxHealth(mob) * HEAL_PCT * HealMultiplier.effective(mob);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + heal));
    }
}
