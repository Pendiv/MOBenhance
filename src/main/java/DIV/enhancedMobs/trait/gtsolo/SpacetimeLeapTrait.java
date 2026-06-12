package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 60秒（1200 tick 固定、ランク非依存）周期で、同ワールド内の最寄りプレイヤー周辺
 * （±5ブロック）へ瞬間移動する。距離・認識は問わない。着地点は最大12回安全判定して選ぶ。
 */
public final class SpacetimeLeapTrait extends Trait {

    /** テレポート周期（原典 1200 tick 固定）。 */
    private static final int INTERVAL = 1200;
    /** プレイヤーからの散布範囲（±5ブロック）。 */
    private static final double SPREAD = 5.0;

    public SpacetimeLeapTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_leap", "STLEAP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        // 付与直後に即跳ばないよう初回周期をセット
        EntityState.setFlag(mob, "st_leap_cd", INTERVAL);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "st_leap_cd")) {
            return;
        }
        // 成否によらず周期を消費する（原典の tickCount % 1200 相当）
        EntityState.setFlag(mob, "st_leap_cd", INTERVAL);
        Player player = Mobs.nearestPlayer(mob, Double.MAX_VALUE);
        if (player == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location base = player.getLocation();
        for (int i = 0; i < 12; i++) {
            Location target = base.clone().add(
                    (random.nextDouble() * 2 - 1) * SPREAD, 0, (random.nextDouble() * 2 - 1) * SPREAD);
            if (!isSafeLanding(target)) {
                continue;
            }
            target.setYaw(mob.getLocation().getYaw());
            target.setPitch(mob.getLocation().getPitch());
            mob.teleport(target);
            mob.getWorld().spawnParticle(Particle.PORTAL, target.clone().add(0, 1, 0), 32, 0.5, 1.0, 0.5);
            mob.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }
    }

    /** 足元が固体で、体の入る2ブロックが通過可能なら着地可。 */
    private static boolean isSafeLanding(Location loc) {
        Block feet = loc.getBlock();
        return feet.isPassable()
                && feet.getRelative(BlockFace.UP).isPassable()
                && feet.getRelative(BlockFace.DOWN).getType().isSolid();
    }
}
