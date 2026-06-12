package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 時空族: 最大HP +50% を得て、毎秒 最大HP×(0.8+0.2×rank)% を自己回復する。
 * 満タンのときは回復分を周囲12mの最も負傷した時空タイプ mob へ回す。
 * リンク先に HP 50% 未満の対象がいれば、自HPを消費して 50% へ漸近補填する
 * （供給上限 = 対象最大HP×2%×rank/秒、自己負担 = 供給×max(0.5, 4.5-0.5×rank)）。
 */
public final class SpacetimeDevotionTrait extends Trait {

    private static final double LINK_RADIUS = 12.0;
    private static final double REGEN_BASE = 0.008;            // (0.8 + 0.2×rank)%/秒
    private static final double REGEN_PER_RANK = 0.002;
    private static final double SUPPLY_PCT_PER_RANK = 0.02;    // 毎秒供給上限 = 対象最大HP 2%×rank
    private static final double COST_MULT_MIN = 0.5;           // 自己消費倍率の下限

    public SpacetimeDevotionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_devotion", "STDEVO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_devotion_hp"),
                0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 原典は20tickごと。tick間隔コンフィグに比例配分して毎秒レートを維持。
        double scale = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval) / 20.0;
        double regen = Mobs.maxHealth(mob) * (REGEN_BASE + REGEN_PER_RANK * rank) * scale;

        LivingEntity healTarget = null;  // 超過回復の振り向け先（負傷中で最も低割合）
        LivingEntity needy = null;       // HP 50% 未満の最困窮リンク先
        double healTargetRatio = Double.MAX_VALUE;
        double needyRatio = Double.MAX_VALUE;
        for (Entity e : mob.getNearbyEntities(LINK_RADIUS, LINK_RADIUS, LINK_RADIUS)) {
            if (!(e instanceof LivingEntity ally) || ally instanceof Player
                    || ally.isDead() || !MobTags.has(ally, "spacetime")) {
                continue;
            }
            double ratio = Mobs.healthRatio(ally);
            if (ally.getHealth() < Mobs.maxHealth(ally) && ratio < healTargetRatio) {
                healTargetRatio = ratio;
                healTarget = ally;
            }
            if (ratio < 0.5 && ratio < needyRatio) {
                needyRatio = ratio;
                needy = ally;
            }
        }

        // 1) 割合回復。満タンなら超過分をリンク味方へ。
        if (mob.getHealth() < Mobs.maxHealth(mob)) {
            heal(mob, regen);
        } else if (healTarget != null) {
            heal(healTarget, regen);
        }

        // 2) 50% 未満のリンク先へ自HPを消費して補填（不足分を上限に漸近）。
        if (needy != null) {
            double deficit = Mobs.maxHealth(needy) * 0.5 - needy.getHealth();
            if (deficit > 0) {
                double give = Math.min(Mobs.maxHealth(needy) * SUPPLY_PCT_PER_RANK * rank * scale, deficit);
                double costMult = Math.max(COST_MULT_MIN, 4.5 - 0.5 * rank);  // 補填の (450-50×rank)% を自己負担
                double cost = give * costMult;
                if (mob.getHealth() - cost > 1) {
                    heal(needy, give);
                    mob.setHealth(mob.getHealth() - cost);
                }
            }
        }
    }

    /** 回復倍率（呪い等）を尊重した回復。 */
    private static void heal(LivingEntity entity, double amount) {
        double healed = amount * HealMultiplier.effective(entity);
        entity.setHealth(Math.min(Mobs.maxHealth(entity), entity.getHealth() + healed));
    }
}
