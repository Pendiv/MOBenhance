package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Periodically deals magic-ish damage to nearby players. */
public final class KillerAuraTrait extends Trait {

    private final double range;
    private final double damagePerRank;

    public KillerAuraTrait(int cost, int weight, int maxRank, int minLevel, double range, double damagePerRank) {
        super("killer_aura", "AURA", cost, weight, maxRank, minLevel);
        this.range = range;
        this.damagePerRank = damagePerRank;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double damage = damagePerRank * rank;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player player && TraitCooldown.ready(player, "killer_aura")) {
                player.damage(damage, mob);
            }
        }
    }
}
