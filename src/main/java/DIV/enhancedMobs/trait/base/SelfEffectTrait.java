package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Trait that grants the mob a lasting self potion effect (PROTECTION, REGEN, INVISIBLE).
 * Mirrors L2Hostility's {@code SelfEffectTrait}.
 */
public class SelfEffectTrait extends Trait {

    // Long but finite so we don't depend on an "infinite duration" API constant.
    private static final int LONG_DURATION = 1_000_000;

    private final PotionEffectType type;
    private final boolean amplifierScalesRank;

    public SelfEffectTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                           PotionEffectType type, boolean amplifierScalesRank) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.type = type;
        this.amplifierScalesRank = amplifierScalesRank;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        int amplifier = amplifierScalesRank ? rank - 1 : 0;
        mob.addPotionEffect(new PotionEffect(type, LONG_DURATION, amplifier, true, false, false));
    }
}
