package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * クリーパー専用（原典 PeerPressure）。自身が膨張を始めると、最寄りプレイヤー（半径12）を
 * 中心とした半径12内の未膨張クリーパーを一斉に着火させる（「プレイヤーの周りが一斉に爆ぜる」）。
 */
public final class PeerPressureTrait extends Trait {

    private static final double SHARE_RADIUS = 12.0;

    public PeerPressureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("peer_pressure", "PEER", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Creeper self) || !swelling(self)) {
            return;
        }
        // 原典: 半径12内の最寄りプレイヤーを中心に連鎖。プレイヤー不在なら何もしない。
        Player player = Mobs.nearestPlayer(mob, SHARE_RADIUS);
        if (player == null) {
            return;
        }
        for (Entity entity : player.getNearbyEntities(SHARE_RADIUS, SHARE_RADIUS, SHARE_RADIUS)) {
            if (entity instanceof Creeper other && other != self && !swelling(other)) {
                other.setIgnited(true);
            }
        }
    }

    /** 膨張プロセス中か（原典の getSwellDir() > 0 相当。自然膨張も含めて検知する）。 */
    private static boolean swelling(Creeper creeper) {
        return creeper.getFuseTicks() > 0 || creeper.isIgnited();
    }
}
