package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 攻撃者に弱体化を与え、自身は力強化を得る（優位を奪い取る）。 */
public final class MysticShadowTrait extends Trait {

    public MysticShadowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("mystic_shadow", "MYSTIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, true, true));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, rank - 1, true, false, false));
        }
    }
}
