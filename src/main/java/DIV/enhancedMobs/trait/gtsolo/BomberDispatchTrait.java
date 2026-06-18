package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/** 一定間隔で着火済みクリーパーを頭上から最近傍プレイヤーに向けて投擲する（爆発で地形は破壊しない）。 */
public final class BomberDispatchTrait extends Trait {

    /** 投擲クリーパーの目印（爆発時に地形破壊を抑止するため {@code MobListener} が参照）。 */
    public static final NamespacedKey THROWN_KEY = new NamespacedKey(EnhancedMobs.get(), "bomber_thrown");

    public BomberDispatchTrait(int cost, int weight, int maxRank, int minLevel) {
        super("bomber_dispatch", "BOMBER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "bomber_cd")) {
            return;
        }
        // 原典の索敵半径 24。
        Player player = Mobs.nearestPlayer(mob, 24);
        if (player == null) {
            return;
        }
        // 原典CD = 240 + 360÷rank tick（lv1 30秒 / lv2 21秒 / lv3 18秒）。
        EntityState.setFlag(mob, "bomber_cd", 240 + 360 / Math.max(1, rank));
        // 頭上(+1.5)に着火済みクリーパーを生成し、プレイヤー方向×速度0.8 + 上向き0.3 補正で射出。
        if (mob.getWorld().spawnEntity(mob.getLocation().add(0, 1.5, 0), EntityType.CREEPER) instanceof Creeper creeper) {
            creeper.setIgnited(true);
            // 投擲クリーパーが本特性を引いた場合の連鎖増殖を防止。
            EnhancedMobs.get().traits().stripTrait(creeper, "bomber_dispatch");
            // 爆発時に地形破壊を消すための目印。
            creeper.getPersistentDataContainer().set(THROWN_KEY, PersistentDataType.BYTE, (byte) 1);
            Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
            if (dir.lengthSquared() > 1e-6) {
                Vector velocity = dir.normalize().multiply(0.8);
                velocity.setY(velocity.getY() + 0.3);
                creeper.setVelocity(velocity);
            }
        }
    }
}
