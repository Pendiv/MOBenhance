package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Survives a lethal hit once, reviving with brief invulnerability. */
public final class SecondSleepTrait extends Trait {

    public SecondSleepTrait(int cost, int weight, int maxRank, int minLevel) {
        super("second_sleep", "2NDSLEEP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "ss_invuln")) {
            event.setCancelled(true);
            return;
        }
        if (EntityState.hasFlag(mob, "ss_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, true, false, false));
        EntityState.setFlag(mob, "ss_invuln", 60);
        EntityState.setFlag(mob, "ss_used", Integer.MAX_VALUE);
    }
}
