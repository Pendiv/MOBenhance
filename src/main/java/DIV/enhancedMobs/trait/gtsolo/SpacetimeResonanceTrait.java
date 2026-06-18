package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * 時空族: 半径8mの時空Mobへ攻撃力上昇と継続回復を与える。対象も共鳴持ちなら効果が大きく増す（相乗）。
 * <ul>
 *   <li>共鳴持ち: 攻撃力 +12.5(N+1)%（MULTIPLY_BASE）+ 最大HPの0.38%/秒 回復</li>
 *   <li>共鳴を持たない時空Mob: 攻撃力 +2.5N% + 1.0HP（0.5ハート）/秒 回復</li>
 * </ul>
 * 攻撃力は固定キーの毎tick貼り替え（原典の transient modifier 付け替えに対応。範囲外に出ると
 * 最終値が残る癖も原典と一致）。回復は HP&lt;max のときのみ。
 */
public final class SpacetimeResonanceTrait extends AuraTrait {

    private static final NamespacedKey ATK_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_resonance_atk");

    public SpacetimeResonanceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_resonance", "STRESO", cost, weight, maxRank, minLevel, 8.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (!MobTags.has(target, "spacetime")) {
            return;
        }
        boolean hasResonance = EnhancedMobs.get().traits().read(target).keySet().stream()
                .anyMatch(t -> t.id().equals(id()));
        // 回復は「毎秒」定義 — tick間隔がコンフィグで変わってもレートが一定になるよう秒換算する。
        double seconds = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval) / 20.0;
        double atkPct;
        double heal;
        if (hasResonance) {
            atkPct = 0.125 * (rank + 1);                       // 共鳴同士の相乗: +12.5(N+1)%
            heal = Mobs.maxHealth(target) * 0.0038 * seconds;  // 最大HP 0.38%/秒
        } else {
            atkPct = 0.025 * rank;                             // +2.5N%
            heal = 1.0 * seconds;                              // 0.5ハート/秒
        }
        Mobs.addModifier(target, Attribute.ATTACK_DAMAGE, ATK_KEY,
                atkPct, AttributeModifier.Operation.ADD_SCALAR);
        Mobs.heal(target, heal); // 回復倍率（封印・呪い）を尊重
    }
}
