package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** 自身が着火されると、周囲のクリーパーも連鎖着火させる。 */
public final class PeerPressureTrait extends Trait {

    public PeerPressureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("peer_pressure", "PEER", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Creeper self) || !self.isIgnited()) {
            return;
        }
        for (Entity entity : mob.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof Creeper other) {
                other.setIgnited(true);
            }
        }
    }
}
