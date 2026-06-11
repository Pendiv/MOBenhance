package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * モブに永続ポーションエフェクトを自己付与するトレイト（PROTECTION, REGEN, INVISIBLE など）。
 * L2Hostility の {@code SelfEffectTrait} に対応する。
 */
public class SelfEffectTrait extends Trait {

    // 有限だが十分に長い値。Paper の "infinite duration" 定数に依存しないための措置。
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
