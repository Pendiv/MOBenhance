package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 10秒ごとに周囲12mの全生物（mob もプレイヤーも）の攻撃力を 50〜200% の一様乱数倍率に
 * 振り直す。倍率は次周期まで持続し、範囲外に出た対象は最後の倍率を保持する（原典仕様）。
 * transient modifier のためサーバー再起動では消える。
 */
public final class SpacetimeConfusionTrait extends Trait {

    private static final NamespacedKey MOD_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_confusion");
    private static final int INTERVAL_TICKS = 200;  // 200tick(10秒)ごとに振り直し
    private static final double RADIUS = 12.0;
    private static final double MIN_FACTOR = 0.5;
    private static final double MAX_FACTOR = 2.0;

    public SpacetimeConfusionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_confusion", "STCONFU", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 周期ゲートは時限フラグ（実 tick 基準）なので tick-interval 設定に依存しない。
        if (EntityState.hasFlag(mob, "st_confusion_cd")) {
            return;
        }
        EntityState.setFlag(mob, "st_confusion_cd", INTERVAL_TICKS);
        for (Entity e : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(e instanceof LivingEntity target)) {
                continue;
            }
            double factor = MIN_FACTOR + ThreadLocalRandom.current().nextDouble() * (MAX_FACTOR - MIN_FACTOR);
            applyTransient(target, factor - 1.0);
        }
    }

    /** 固定キーの transient modifier を付け替える（原典の MULTIPLY_TOTAL ≒ MULTIPLY_SCALAR_1）。 */
    private static void applyTransient(LivingEntity target, double amount) {
        AttributeInstance inst = target.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> MOD_KEY.equals(m.getKey())).toList().forEach(inst::removeModifier);
        inst.addTransientModifier(new AttributeModifier(MOD_KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
