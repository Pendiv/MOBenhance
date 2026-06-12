package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 膨張（着火プロセス）中、近くのプレイヤーを毎tick自身の方向へ引き寄せるクリーパー。 */
public final class CentripetalForceTrait extends Trait {

    /** 毎tickの吸引強度（原典 PULL 固定値）。 */
    private static final double PULL = 0.06;

    public CentripetalForceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("centripetal_force", "CENTRI", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 膨張中のみ1tick周期レーンで吸引（20tickで自然終了し、継続中なら次のtrait tickが再点火）
        if (!(mob instanceof Creeper creeper) || !swelling(creeper)) {
            return;
        }
        double radius = 3.0 + 2.0 * rank;
        int[] life = {20};
        FastTick.register(creeper, "centripetal_force", () -> {
            if (!creeper.isValid() || !swelling(creeper) || --life[0] < 0) {
                return false;
            }
            pull(creeper, radius);
            return true;
        });
    }

    /** 膨張プロセス中か（原典の getSwellDir() > 0 相当）。 */
    private static boolean swelling(Creeper creeper) {
        return creeper.getFuseTicks() > 0 || creeper.isIgnited();
    }

    private static void pull(Creeper creeper, double radius) {
        for (Entity entity : creeper.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                continue;
            }
            Vector dir = creeper.getLocation().toVector().subtract(player.getLocation().toVector());
            double dist = dir.length();
            if (dist < 0.5 || dist > radius) {
                continue;
            }
            player.setVelocity(player.getVelocity().add(dir.multiply(PULL / dist)));
        }
    }
}
