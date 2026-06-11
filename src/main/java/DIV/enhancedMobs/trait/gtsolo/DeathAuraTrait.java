package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

/** 周囲のプレイヤーに定期的にダメージと着火を与える（プレイヤー個別のクールダウンあり）。 */
public final class DeathAuraTrait extends AuraTrait {

    public DeathAuraTrait(int cost, int weight, int maxRank, int minLevel) {
        super("death_aura", "DTHAURA", cost, weight, maxRank, minLevel, 5.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (!TraitCooldown.ready(target, "death_aura")) {
            return;
        }
        target.damage(1.5 * rank, mob);
        target.setFireTicks(Math.max(target.getFireTicks(), 40));
    }
}
