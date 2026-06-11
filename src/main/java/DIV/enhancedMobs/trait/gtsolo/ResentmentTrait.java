package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 死亡時、周囲8マスのプレイヤーに弱体化と移動速度低下を付与する。 */
public final class ResentmentTrait extends Trait {

    public ResentmentTrait(int cost, int weight, int maxRank, int minLevel) {
        super("resentment", "RESENT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int duration = 60 * rank;
        for (Entity entity : mob.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, rank - 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, rank - 1, true, true, true));
            }
        }
    }
}
