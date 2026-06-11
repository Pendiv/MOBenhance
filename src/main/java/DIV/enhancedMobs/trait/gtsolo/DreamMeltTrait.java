package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 致死ダメージを一度だけ無効化し、全回復して短時間透明になる（1回限り）。 */
public final class DreamMeltTrait extends Trait {

    public DreamMeltTrait(int cost, int weight, int maxRank, int minLevel) {
        super("dream_melt", "DREAM", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "dream_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, true, false, false));
        EntityState.setFlag(mob, "dream_used", Integer.MAX_VALUE);
    }
}
