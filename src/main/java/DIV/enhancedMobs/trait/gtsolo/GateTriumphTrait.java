package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 門と道の凱旋 — 120秒ごとに、同ワールドの最寄りプレイヤー（スペクテイター/クリエイティブ除外、
 * 距離無制限）から16〜32mの足場（足元固体+2マス空気、y±12走査、だめなら地表）へテレポートする。
 * テレポ前に周囲12mのモブを収集して行き先±4mへ引き連れ、随行ゼロなら行き先バイオームに
 * 適したモンスターを3〜5体召喚する（召喚体は本特性を剥奪して連鎖召喚を防ぐ）。
 */
public final class GateTriumphTrait extends Trait {

    private static final int INTERVAL = 2400;
    private static final double ENTOURAGE_RADIUS = 12.0;

    public GateTriumphTrait(int cost, int weight, int maxRank, int minLevel) {
        super("gate_triumph", "GATE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典は tickCount 起算（スポーン後最大120秒待ち）。初tickでの即テレポを防ぐ。
        EntityState.setFlag(mob, "gate_cd", INTERVAL);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "gate_cd")) {
            return;
        }
        EntityState.setFlag(mob, "gate_cd", INTERVAL);

        Player target = nearestEligiblePlayer(mob);
        if (target == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location dest = pickDestination(target, random);
        if (dest == null) {
            return;
        }

        // 随行モブはテレポ前に収集する（テレポ後だと半径の基準が変わるため）。
        List<Mob> entourage = new ArrayList<>();
        for (Entity entity : mob.getNearbyEntities(ENTOURAGE_RADIUS, ENTOURAGE_RADIUS, ENTOURAGE_RADIUS)) {
            if (entity instanceof Mob other && !other.isDead()) {
                entourage.add(other);
            }
        }

        Mobs.teleport(mob, dest);

        if (entourage.isEmpty()) {
            summonBiomeMobs(dest, random);
        } else {
            for (Mob follower : entourage) {
                Mobs.teleport(follower, nearbyGround(dest, random, 4));
            }
        }
    }

    /** 同ワールドの最寄り生存プレイヤー（スペクテイター/クリエイティブ除外、距離無制限）。 */
    private static Player nearestEligiblePlayer(LivingEntity mob) {
        Player best = null;
        double bestSq = Double.MAX_VALUE;
        for (Player player : mob.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR
                    || player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            double distSq = player.getLocation().distanceSquared(mob.getLocation());
            if (distSq < bestSq) {
                bestSq = distSq;
                best = player;
            }
        }
        return best;
    }

    /** プレイヤーから16〜32mのランダム方位で足場のある地点を8回試行で探す。 */
    private static Location pickDestination(Player player, ThreadLocalRandom random) {
        World world = player.getWorld();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 16.0 + random.nextDouble() * 16.0;
            int x = (int) Math.round(player.getLocation().getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(player.getLocation().getZ() + Math.sin(angle) * dist);
            Location ground = findGround(world, x, player.getLocation().getBlockY(), z);
            if (ground != null) {
                return ground;
            }
        }
        return null;
    }

    /** y±12 を走査し「足元固体 + 2マス空気」の位置を探す。見つからなければ地表高度。 */
    private static Location findGround(World world, int x, int y, int z) {
        for (int dy = 0; dy <= 12; dy++) {
            for (int sign : new int[]{1, -1}) {
                Location loc = standable(world, x, y + dy * sign, z);
                if (loc != null) {
                    return loc;
                }
                if (dy == 0) {
                    break;
                }
            }
        }
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        return standable(world, x, surfaceY, z);
    }

    private static Location standable(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getRelative(BlockFace.DOWN).getType().isSolid()
                && block.isPassable() && block.getRelative(BlockFace.UP).isPassable()) {
            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }

    /** 中心±spread の足場を6回試行で探す。見つからなければ中心。 */
    private static Location nearbyGround(Location center, ThreadLocalRandom random, int spread) {
        for (int i = 0; i < 6; i++) {
            int x = center.getBlockX() + random.nextInt(spread * 2 + 1) - spread;
            int z = center.getBlockZ() + random.nextInt(spread * 2 + 1) - spread;
            Location loc = findGround(center.getWorld(), x, center.getBlockY(), z);
            if (loc != null) {
                return loc;
            }
        }
        return center;
    }

    /** 行き先バイオームに適したモンスターを3〜5体召喚する（レベリング・特性付与は spawn イベント側）。 */
    private void summonBiomeMobs(Location dest, ThreadLocalRandom random) {
        World world = dest.getWorld();
        EntityType[] table = biomeMonsters(world, dest);
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            EntityType type = table[random.nextInt(table.length)];
            Entity spawned = world.spawnEntity(nearbyGround(dest, random, 4), type);
            if (spawned instanceof LivingEntity living) {
                // 安全装置: 召喚体が本特性を引いて連鎖召喚しないよう剥奪する。
                EnhancedMobs.get().traits().stripTrait(living, id());
            }
        }
    }

    /**
     * バイオーム→モンスターの静的テーブル（Bukkit に湧きリストAPIが無いための近似）。
     * キー文字列マッチでバージョン間のバイオーム定数差異を回避する。
     */
    private static EntityType[] biomeMonsters(World world, Location dest) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            Biome biome = world.getBiome(dest);
            String key = biome.getKey().getKey();
            if (key.contains("soul_sand")) {
                return new EntityType[]{EntityType.SKELETON, EntityType.GHAST};
            }
            if (key.contains("basalt")) {
                return new EntityType[]{EntityType.MAGMA_CUBE};
            }
            if (key.contains("warped")) {
                return new EntityType[]{EntityType.ENDERMAN};
            }
            return new EntityType[]{EntityType.ZOMBIFIED_PIGLIN, EntityType.MAGMA_CUBE};
        }
        if (world.getEnvironment() == World.Environment.THE_END) {
            return new EntityType[]{EntityType.ENDERMAN};
        }
        Biome biome = world.getBiome(dest);
        String key = biome.getKey().getKey();
        if (key.contains("desert")) {
            return new EntityType[]{EntityType.HUSK, EntityType.ZOMBIE, EntityType.SKELETON};
        }
        if (key.contains("snowy") || key.contains("frozen") || key.contains("ice")) {
            return new EntityType[]{EntityType.STRAY, EntityType.ZOMBIE, EntityType.SPIDER};
        }
        if (key.contains("ocean") || key.contains("river")) {
            return new EntityType[]{EntityType.DROWNED};
        }
        if (key.contains("swamp") || key.contains("mangrove")) {
            return new EntityType[]{EntityType.ZOMBIE, EntityType.SLIME, EntityType.WITCH};
        }
        return new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER};
    }
}
