package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 最寄りのプレイヤーへ向かって突進するクリーパー。 */
public final class LovesickTrait extends Trait {

    public LovesickTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lovesick", "LOVE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        Player player = Mobs.nearestPlayer(mob, 16);
        if (player == null) {
            return;
        }
        Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (dir.lengthSquared() > 0.01) {
            mob.setVelocity(mob.getVelocity().add(dir.normalize().multiply(0.2 * rank)));
        }
    }
}
