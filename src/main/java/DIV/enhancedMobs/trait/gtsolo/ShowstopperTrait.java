package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 常時発光・炎上状態だが、火炎耐性により火ダメージは受けない。 */
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
