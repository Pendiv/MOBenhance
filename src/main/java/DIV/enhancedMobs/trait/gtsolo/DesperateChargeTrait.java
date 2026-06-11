package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** プレイヤーへの命中時に弱体化・鈍足・採掘疲労を付与する。 */
public final class DesperateChargeTrait extends Trait {

    public DesperateChargeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("desperate_charge", "DESPER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player)) {
            return;
        }
        int duration = 60 * rank;
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, rank - 1, true, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, rank - 1, true, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, rank - 1, true, true, true));
    }
}
