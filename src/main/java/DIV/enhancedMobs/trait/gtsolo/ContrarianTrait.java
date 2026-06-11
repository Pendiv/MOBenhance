package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 火・溶岩・高温床によるダメージを無効化するクリーパー。 */
public final class ContrarianTrait extends Trait {

    public ContrarianTrait(int cost, int weight, int maxRank, int minLevel) {
        super("contrarian", "CONTRA", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> event.setCancelled(true);
            default -> {
            }
        }
    }
}
