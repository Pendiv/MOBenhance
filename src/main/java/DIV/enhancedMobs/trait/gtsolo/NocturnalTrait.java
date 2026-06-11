package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Empowered during the night. */
public final class NocturnalTrait extends Trait {

    public NocturnalTrait(int cost, int weight, int maxRank, int minLevel) {
        super("nocturnal", "NOCT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob.getWorld().getTime() % 24000 < 12000) {
            return; // day
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, false));
    }
}
