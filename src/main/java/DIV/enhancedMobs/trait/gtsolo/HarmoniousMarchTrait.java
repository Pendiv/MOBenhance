package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * 調和の行軍 — 自身は HP×1.5・攻撃力 -(36+18×rank)% になる代わりに、半径8の自分以外のモブへ
 * 「自身の攻撃力base × (12+6×rank)%」を固定加算バフとして配るサポーター
 * （強いモブが配るほど強い）。バフは範囲外に出ても即時には消えない（原典 v1 仕様の残留を再現）。
 */
public final class HarmoniousMarchTrait extends AuraTrait {

    public HarmoniousMarchTrait(int cost, int weight, int maxRank, int minLevel) {
        super("harmonious_march", "MARCH", cost, weight, maxRank, minLevel, 8.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(EnhancedMobs.get(), "trait_march_hp"),
                0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(EnhancedMobs.get(), "trait_march_atk"),
                -(0.36 + 0.18 * rank), AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        AttributeInstance own = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (own == null) {
            return;
        }
        // 自身の攻撃力 base に比例した固定加算（remove→add の冪等更新）。
        double buff = own.getBaseValue() * (0.12 + 0.06 * rank);
        Mobs.addModifier(target, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_march_aura"),
                buff, AttributeModifier.Operation.ADD_NUMBER);
    }
}
