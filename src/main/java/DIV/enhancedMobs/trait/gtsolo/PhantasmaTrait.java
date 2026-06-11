package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 被弾3回ごとに1回だけダメージを無効化する。 */
public final class PhantasmaTrait extends Trait {

    public PhantasmaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantasma", "PHANTASMA", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.addInt(mob, "phantasma_hits", 1) % 3 == 0) {
            event.setCancelled(true);
        }
    }
}
