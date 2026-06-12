package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 魔法ダメージを無効化し、その半量を回復する。 */
public final class MagicalCreaturesTrait extends Trait {

    public MagicalCreaturesTrait(int cost, int weight, int maxRank, int minLevel) {
        super("magical_creatures", "MAGIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 原典の WITCH_RESISTANT_TO タグ相当: magic / indirect_magic / sonic_boom / thorns
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SONIC_BOOM
                || cause == EntityDamageEvent.DamageCause.THORNS) {
            event.setCancelled(true);
            mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + event.getDamage() * 0.5));
        }
    }
}
