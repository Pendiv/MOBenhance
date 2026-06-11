package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Steals the attacker's edge: weakens them and empowers itself. */
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
