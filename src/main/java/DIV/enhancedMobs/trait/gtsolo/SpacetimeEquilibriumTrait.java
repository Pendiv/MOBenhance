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
import org.bukkit.entity.Player;

/**
 * 時空族: 周囲12mのプレイヤーおよび時空タイプ mob の攻撃力を、時間とともに徐々に下げる。
 * 1層 = 攻撃力 -2.5×rank%、5秒ごとに1層深まり、上限 (15+10×rank) 層。
 * 減少率は固定キーの transient modifier として毎周期付け替える
 * （範囲外に出た対象は最後の値が残る = 原典仕様）。
 */
public final class SpacetimeEquilibriumTrait extends Trait {

    private static final NamespacedKey MOD_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_equilibrium");
    private static final String RAMP_KEY = "equi_ramp";
    private static final double RADIUS = 12.0;
    private static final int TICKS_PER_LAYER = 100;               // 5秒ごとに1層
    private static final double PCT_PER_LAYER_PER_RANK = 0.025;   // 1層 = -2.5×rank%

    public SpacetimeEquilibriumTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_equilibrium", "STEQUI", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // ramp は経過 tick 数で蓄積（tick間隔コンフィグに依存せず原典の「5秒で1層」を維持）。
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        int maxLayers = 15 + 10 * rank;
        int ramp = Math.min(EntityState.getInt(mob, RAMP_KEY, 0) + interval, maxLayers * TICKS_PER_LAYER);
        EntityState.setInt(mob, RAMP_KEY, ramp);
        int layers = ramp / TICKS_PER_LAYER;
        double reduction = layers * PCT_PER_LAYER_PER_RANK * rank;
        if (reduction <= 0) {
            return;
        }
        for (Entity e : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(e instanceof LivingEntity target)) {
                continue;
            }
            if (!(target instanceof Player) && !MobTags.has(target, "spacetime")) {
                continue;
            }
            applyTransient(target, -reduction);
        }
    }

    /** 固定キーの transient modifier を付け替える（原典の MULTIPLY_BASE ≒ MULTIPLY_SCALAR_1）。 */
    private static void applyTransient(LivingEntity target, double amount) {
        AttributeInstance inst = target.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> MOD_KEY.equals(m.getKey())).toList().forEach(inst::removeModifier);
        inst.addTransientModifier(new AttributeModifier(MOD_KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
