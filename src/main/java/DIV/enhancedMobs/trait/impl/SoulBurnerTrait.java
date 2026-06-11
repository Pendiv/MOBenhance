package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装 SOUL_BURNER: ヒット時に対象を着火しウィザー効果を付与する。 */
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
