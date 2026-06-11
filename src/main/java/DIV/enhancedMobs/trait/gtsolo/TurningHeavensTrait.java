package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.HealMultiplier;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

/** 近傍プレイヤーにダメージを与えつつ回復を封じるオーラ。 */
public final class TurningHeavensTrait extends AuraTrait {

    public TurningHeavensTrait(int cost, int weight, int maxRank, int minLevel) {
        super("turning_heavens", "HEAVENS", cost, weight, maxRank, minLevel, 12.0, TargetKind.PLAYERS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.damage(rank, mob);
        HealMultiplier.applyCurse(target, 0, 40);
    }
}
