package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 一定間隔で着火済みクリーパーを最近傍プレイヤーに向けて射出する。 */
public final class BomberDispatchTrait extends Trait {

    public BomberDispatchTrait(int cost, int weight, int maxRank, int minLevel) {
        super("bomber_dispatch", "BOMBER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "bomber_cd")) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 32);
        if (player == null) {
            return;
        }
        if (mob.getWorld().spawnEntity(mob.getLocation(), EntityType.CREEPER) instanceof Creeper creeper) {
            creeper.setIgnited(true);
            Vector dir = player.getLocation().toVector().subtract(creeper.getLocation().toVector());
            if (dir.lengthSquared() > 1e-6) {
                creeper.setVelocity(dir.normalize().multiply(0.6));
            }
        }
        EntityState.setFlag(mob, "bomber_cd", 40);
    }
}
