package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

/**
 * 奈落歩き: 奈落落下（ビルド下限未満）を検知し、近傍の地表へ螺旋探索でテレポートして速度をゼロ化する。
 * 探索はロード済みチャンク限定・半径128（原典は250だが、同期チャンクロードでサーバが止まるのを
 * 防ぐための近似縮小）。地表が見つからなければワールドスポーンへ退避する。
 * tick粒度（既定1秒）の間に奈落ダメージで死なないよう、VOIDダメージはキャンセルする。
 */
public final class WalkingAbyssTrait extends Trait {

    /** 地表探索の最大半径（原典 SEARCH_RADIUS=250 のロード済み限定近似）。 */
    private static final int SEARCH_RADIUS = 128;

    public WalkingAbyssTrait(int cost, int weight, int maxRank, int minLevel) {
        super("walking_abyss", "ABYSS", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        World world = mob.getWorld();
        if (mob.getLocation().getY() >= world.getMinHeight()) {
            return;
        }
        Location origin = mob.getLocation();
        Location safe = findNearbyLand(world, origin.getBlockX(), origin.getBlockZ());
        Mobs.teleport(mob, safe != null ? safe : world.getSpawnLocation());
        mob.setVelocity(new Vector());
    }

    /** 螺旋状（チェビシェフ環）に各(x,z)のWORLD_SURFACE最上面を調べ、最初の固体ブロックの上を返す。 */
    private static Location findNearbyLand(World world, int ox, int oz) {
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                if (Math.abs(dx) == r) {
                    // 環の左右辺: dz 全域を走査。
                    for (int dz = -r; dz <= r; dz++) {
                        Location found = checkColumn(world, ox + dx, oz + dz);
                        if (found != null) {
                            return found;
                        }
                    }
                } else {
                    // 環の上下辺のみ（内側は既探索）。
                    Location found = checkColumn(world, ox + dx, oz - r);
                    if (found == null) {
                        found = checkColumn(world, ox + dx, oz + r);
                    }
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /** (x,z) 列の最上面が固体ならその上の着地点を返す。未ロードチャンクはスキップ。 */
    private static Location checkColumn(World world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        int y = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        if (y <= world.getMinHeight()) {
            return null;
        }
        if (!world.getBlockAt(x, y, z).getType().isSolid()) {
            return null;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
}
