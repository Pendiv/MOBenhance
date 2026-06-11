package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Weakens nearby mobs. */
public final class LazinessTrait extends AuraTrait {

    public LazinessTrait(int cost, int weight, int maxRank, int minLevel) {
        super("laziness", "LAZY", cost, weight, maxRank, minLevel, 8.0, TargetKind.MOBS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, rank - 1, true, false, false));
    }
}
