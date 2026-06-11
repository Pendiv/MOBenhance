package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 近似実装: 魔法ダメージ無効＋耐性、致死ダメージを1度だけ全回復で踏みとどまる。 */
public final class TrinityLifeTrait extends Trait {

    public TrinityLifeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("trinity_life", "TRINITY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, 1, true, false, false));
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            event.setCancelled(true);
            return;
        }
        if (EntityState.hasFlag(mob, "trinity_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        EntityState.setFlag(mob, "trinity_used", Integer.MAX_VALUE);
    }
}
