package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Empowered while it has no mob company nearby (loves the spotlight). */
public final class AudienceEffectTrait extends Trait {

    public AudienceEffectTrait(int cost, int weight, int maxRank, int minLevel) {
        super("audience_effect", "AUDIENCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        for (Entity entity : mob.getNearbyEntities(16, 16, 16)) {
            if (entity instanceof Mob) {
                return;
            }
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false, false));
    }
}
