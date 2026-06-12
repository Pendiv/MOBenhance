package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 時空族: 半径 4 + rank 内のプレイヤーおよび時空タイプでないMobに、毎秒
 * 自身の攻撃力 × (10 + 5rank)% の帰属なしダメージを与える排斥オーラ。
 */
public final class SpacetimeRejectionTrait extends Trait {

    /** 攻撃力属性がない場合のフォールバック値（原典 2.0）。 */
    private static final double FALLBACK_ATK = 2.0;

    public SpacetimeRejectionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_rejection", "STREJ", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double radius = 4.0 + rank;
        AttributeInstance atkInst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        double atk = atkInst != null ? atkInst.getValue() : FALLBACK_ATK;
        double dmg = atk * (0.10 + 0.05 * rank);
        if (dmg <= 0) {
            return;
        }
        for (Entity entity : mob.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            // プレイヤーは常に、Mobは時空タイプでない場合のみ排斥
            if (target instanceof Player || !MobTags.has(target, "spacetime")) {
                target.damage(dmg); // 帰属なし（原典は magic 種別・攻撃者なし）
            }
        }
    }
}
