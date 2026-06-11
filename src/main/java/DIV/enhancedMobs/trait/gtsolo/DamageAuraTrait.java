package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

/** 近くのプレイヤーに定期的にダメージを与えるオーラ（プレイヤーごとにクールダウン管理）。 */
public final class DamageAuraTrait extends AuraTrait {

    public DamageAuraTrait(int cost, int weight, int maxRank, int minLevel) {
        super("damage_aura", "DMGAURA", cost, weight, maxRank, minLevel, 4.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (!TraitCooldown.ready(target, "damage_aura")) {
            return;
        }
        target.damage(rank, mob);
    }
}
