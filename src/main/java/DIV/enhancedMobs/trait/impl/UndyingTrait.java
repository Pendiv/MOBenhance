package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

/** Survives one otherwise-lethal hit, restoring full health. */
public final class UndyingTrait extends Trait {

    private final NamespacedKey usedKey;

    public UndyingTrait(int cost, int weight, int maxRank, int minLevel) {
        super("undying", "UNDYING", cost, weight, maxRank, minLevel);
        this.usedKey = new NamespacedKey(EnhancedMobs.get(), "undying_used");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (mob.getPersistentDataContainer().has(usedKey, PersistentDataType.BYTE)) {
            return;
        }
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        mob.setHealth(maxHealth != null ? maxHealth.getValue() : mob.getHealth());
        mob.getPersistentDataContainer().set(usedKey, PersistentDataType.BYTE, (byte) 1);
    }
}
