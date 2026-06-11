package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 炎耐性を持ち、炎上中は筋力が付与される。火・溶岩ダメージを受けると炎上が延長される。 */
public final class BurningPassionTrait extends Trait {

    public BurningPassionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("burning_passion", "PASSION", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1_000_000, 0, true, false, false));
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA -> mob.setFireTicks(1_000_000);
            default -> {
            }
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob.getFireTicks() > 0) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        }
    }
}
