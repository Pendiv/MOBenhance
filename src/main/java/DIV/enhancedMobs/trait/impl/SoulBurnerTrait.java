package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Compromise SOUL_BURNER: sets the target on fire and applies wither on hit. */
public final class SoulBurnerTrait extends Trait {

    private final int fireSeconds;
    private final int witherTicks;

    public SoulBurnerTrait(int cost, int weight, int maxRank, int minLevel, int fireSeconds, int witherTicks) {
        super("soul_burner", "SOUL", cost, weight, maxRank, minLevel);
        this.fireSeconds = fireSeconds;
        this.witherTicks = witherTicks;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }
        target.setFireTicks(Math.max(target.getFireTicks(), fireSeconds * 20 * rank));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks * rank, 0, true, true, true));
    }
}
