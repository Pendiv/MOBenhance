package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Applies hunger to nearby players. */
public final class FamineTrait extends AuraTrait {

    public FamineTrait(int cost, int weight, int maxRank, int minLevel) {
        super("famine", "FAMINE", cost, weight, maxRank, minLevel, 16.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, rank - 1, true, true, true));
    }
}
