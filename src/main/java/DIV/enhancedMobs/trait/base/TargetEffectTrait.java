package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * モブが攻撃した相手にポーションエフェクトを付与するトレイト（POISON, WITHER, SLOWNESS など）。
 * L2Hostility の {@code TargetEffectTrait} に対応する。
 */
public class TargetEffectTrait extends Trait {

    private final PotionEffectType type;
    private final int baseDuration;
    private final boolean durationScalesRank;
    private final boolean amplifierScalesRank;

    public TargetEffectTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                             PotionEffectType type, int baseDuration,
                             boolean durationScalesRank, boolean amplifierScalesRank) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.type = type;
        this.baseDuration = baseDuration;
        this.durationScalesRank = durationScalesRank;
        this.amplifierScalesRank = amplifierScalesRank;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }
        int duration = durationScalesRank ? baseDuration * rank : baseDuration;
        int amplifier = amplifierScalesRank ? rank - 1 : 0;
        target.addPotionEffect(new PotionEffect(type, duration, amplifier, true, true, true));
    }
}
