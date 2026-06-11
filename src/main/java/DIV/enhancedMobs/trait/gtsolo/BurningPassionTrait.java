package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Permanently burns and is empowered while on fire; immune to fire. */
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
