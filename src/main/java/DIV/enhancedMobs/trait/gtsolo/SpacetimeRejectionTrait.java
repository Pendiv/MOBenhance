package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** 近傍のプレイヤーおよびspacetimeタグを持たないMobに継続ダメージを与えるオーラ。 */
public final class SpacetimeRejectionTrait extends AuraTrait {

    public SpacetimeRejectionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_rejection", "STREJ", cost, weight, maxRank, minLevel, 6.0, TargetKind.ALL);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (target instanceof Player || !MobTags.has(target, "spacetime")) {
            target.damage(rank, mob);
        }
    }
}
