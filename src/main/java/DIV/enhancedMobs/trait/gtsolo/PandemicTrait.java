package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Spreads poison to nearby players. */
public final class PandemicTrait extends AuraTrait {

    public PandemicTrait(int cost, int weight, int maxRank, int minLevel) {
        super("pandemic", "PANDEMIC", cost, weight, maxRank, minLevel, 8.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, rank - 1, true, true, true));
    }
}
