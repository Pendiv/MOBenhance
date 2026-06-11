package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/** Compromise DRAIN: steals one beneficial potion effect from the target and gains it. */
public final class DrainTrait extends Trait {

    private static final Set<PotionEffectType> BENEFICIAL = Set.of(
            PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION,
            PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.ABSORPTION,
            PotionEffectType.JUMP_BOOST, PotionEffectType.HASTE);

    public DrainTrait(int cost, int weight, int maxRank, int minLevel) {
        super("drain", "DRAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (BENEFICIAL.contains(effect.getType())) {
                target.removePotionEffect(effect.getType());
                mob.addPotionEffect(effect);
                return;
            }
        }
    }
}
