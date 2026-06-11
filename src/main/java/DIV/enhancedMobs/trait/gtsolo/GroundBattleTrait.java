package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

/** 近くのプレイヤーから浮遊・低速落下を除去して地面に縛り付ける。 */
public final class GroundBattleTrait extends AuraTrait {

    public GroundBattleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("ground_battle", "GROUND", cost, weight, maxRank, minLevel, 32.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.removePotionEffect(PotionEffectType.LEVITATION);
        target.removePotionEffect(PotionEffectType.SLOW_FALLING);
    }
}
