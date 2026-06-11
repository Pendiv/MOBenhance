package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 初期は高速移動するが、プレイヤーに攻撃されると速度効果が解除される。 */
public final class CarriedAwayTrait extends Trait {

    public CarriedAwayTrait(int cost, int weight, int maxRank, int minLevel) {
        super("carried_away", "CARRIED", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1_000_000, 2 + rank, true, false, false));
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            mob.removePotionEffect(PotionEffectType.SPEED);
        }
    }
}
