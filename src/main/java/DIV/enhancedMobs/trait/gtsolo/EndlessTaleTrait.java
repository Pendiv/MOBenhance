package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 致死ダメージ時に確率でフルHP復活する。初回確率は 50% + 22.15%×rank（rank3 で確定）。
 * 復活成功のたびに確率が ×0.8 − 5% に減衰し、0 以下になると以後復活しない。
 */
public final class EndlessTaleTrait extends Trait {

    public EndlessTaleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("endless_tale", "ENDLESS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        double chance = EntityState.getDouble(mob, "et_chance", 0.50 + 0.2215 * rank);
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        // 減衰は成功時のみ（原典: currentChance = c×0.8 − 0.05）
        EntityState.setDouble(mob, "et_chance", chance * 0.8 - 0.05);
    }
}
