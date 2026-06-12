package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 着火（膨張）中のみ、最寄りのプレイヤーへ毎tickじわじわ水平追尾するクリーパー。 */
public final class LovesickTrait extends Trait {

    private static final double SEARCH_RADIUS = 24.0;

    public LovesickTrait(int cost, int weight, int maxRank, int minLevel) {
        super("lovesick", "LOVE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 膨張中のみ1tick周期レーンで追尾（20tickで自然終了し、継続中なら次のtrait tickが再点火）
        if (!(mob instanceof Creeper creeper) || !swelling(creeper)) {
            return;
        }
        int[] life = {20};
        FastTick.register(creeper, "lovesick", () -> {
            if (!creeper.isValid() || !swelling(creeper) || --life[0] < 0) {
                return false;
            }
            chase(creeper, rank);
            return true;
        });
    }

    /** 膨張プロセス中か（原典の getSwellDir() > 0 相当）。 */
    private static boolean swelling(Creeper creeper) {
        return creeper.getFuseTicks() > 0 || creeper.isIgnited();
    }

    /** プレイヤー移動速度 × 0.2 × rank を水平成分のみ毎tick加算（原典式）。 */
    private static void chase(Creeper creeper, int rank) {
        Player player = Mobs.nearestPlayer(creeper, SEARCH_RADIUS);
        if (player == null) {
            return;
        }
        Vector dir = player.getLocation().toVector().subtract(creeper.getLocation().toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 1e-4) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        double amount = (speed != null ? speed.getValue() : 0.1) * 0.2 * rank;
        creeper.setVelocity(creeper.getVelocity().add(dir.normalize().multiply(amount)));
    }
}
