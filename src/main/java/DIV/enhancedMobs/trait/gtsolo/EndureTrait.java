package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 致死ダメージを一度だけ1HPで耐え、直後にResistance Vを短時間付与する（1回限り）。 */
public final class EndureTrait extends Trait {

    public EndureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("endure", "ENDURE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // /kill・奈落は素通し（原典の BYPASSES_INVULNERABILITY 相当）
        if (event.getCause() == EntityDamageEvent.DamageCause.KILL
                || event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        if (EntityState.hasFlag(mob, "endure_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(1);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, true, true, true));
        EntityState.setFlag(mob, "endure_used", Integer.MAX_VALUE);
    }
}
