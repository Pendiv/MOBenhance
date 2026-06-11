package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Always glowing and burning, but fire-resistant. */
public final class ShowstopperTrait extends Trait {

    public ShowstopperTrait(int cost, int weight, int maxRank, int minLevel) {
        super("showstopper", "SHOW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1_000_000, 0, true, false, false));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        mob.setFireTicks(Math.max(mob.getFireTicks(), 100));
    }
}
