package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

/** Continuously shrugs off harmful potion effects. */
public final class PureHeartTrait extends Trait {

    private static final PotionEffectType[] HARMFUL = {
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA, PotionEffectType.LEVITATION
    };

    public PureHeartTrait(int cost, int weight, int maxRank, int minLevel) {
        super("pure_heart", "PURE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        for (PotionEffectType type : HARMFUL) {
            if (mob.hasPotionEffect(type)) {
                mob.removePotionEffect(type);
            }
        }
    }
}
