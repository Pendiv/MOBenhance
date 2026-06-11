package DIV.enhancedMobs.level;

import DIV.enhancedMobs.config.DimensionConfig;
import DIV.enhancedMobs.config.LocationConfig;
import DIV.enhancedMobs.config.MainConfig;
import DIV.enhancedMobs.core.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes a mob's level from a layered "danger" model.
 *
 * <pre>
 * danger = (D_global + D_personal) * T + offset(reference)      [clamp for mobs]
 *
 * D_global   = base
 *            + distanceFactor * distanceFromWorldSpawn
 *            + floor( cbrt( sum of all online players' MOB_KILLS ) )
 *            + netherValue * (online who visited Nether / online total)
 *            + endValue    * (online who visited End    / online total)
 *            + gaussian * variation                              (mob spawns only)
 *
 * D_personal = cbrt( sum_i  X_i / N_i^1.6 ) * I                  ("gravity" + importance)
 *   X_i = player i's MOB_KILLS,  N_i = chunk distance player i -> mob (min 1),
 *   over players in the mob's world only.
 *   I   = K0 / (K0 + c),  K0 = reference player's MOB_KILLS.
 *
 * T          = round1( 1 + sqrt(Y) / 12 ),  Y = elapsed days of the primary world.
 * offset     = the reference player's manual difficulty offset (set by OPs).
 * </pre>
 *
 * <p>For a spawning mob the reference is the nearest player; for a player's own readout the
 * reference is that player. Both share this method so the number a player sees matches what
 * mobs around them experience.
 */
public final class DifficultyCalculator {

    private static final double CHUNK = 16.0;
    private static final double GRAVITY_EXP = 1.6;

    private final MainConfig config;
    private final DimensionConfig dimensions;
    private final LocationConfig locations;

    public DifficultyCalculator(MainConfig config, DimensionConfig dimensions, LocationConfig locations) {
        this.config = config;
        this.dimensions = dimensions;
        this.locations = locations;
    }

    /** Level for a spawning mob (with random wobble), clamped to [0, maxMobLevel]. */
    public int compute(Location loc) {
        Player reference = nearestPlayer(loc);
        int level = (int) Math.round(rawDanger(loc, reference, true));
        level = dimensions.scaleLevel(loc.getWorld(), level);
        level = (int) Math.round(level * locations.multiplier(loc));
        level = (int) Math.round(level * config.finalMultiplier);
        if (level < 0) {
            level = 0;
        }
        if (level > config.maxMobLevel) {
            level = config.maxMobLevel;
        }
        return level;
    }

    /** Deterministic difficulty readout for a player (no random wobble), floored at 0. */
    public int playerDifficulty(Player player) {
        int value = (int) Math.round(rawDanger(player.getLocation(), player, false));
        value = dimensions.scaleLevel(player.getWorld(), value);
        value = (int) Math.round(value * locations.multiplier(player.getLocation()));
        value = (int) Math.round(value * config.finalMultiplier);
        return Math.max(0, value);
    }

    private double rawDanger(Location loc, Player reference, boolean wobble) {
        double core = (globalDanger(loc, reference, wobble) + personalDanger(loc, reference)) * dayFactor();
        double offset = reference == null ? 0.0 : PlayerData.of(reference).getDifficultyOffset();
        return core + offset;
    }

    private double globalDanger(Location loc, Player reference, boolean wobble) {
        double danger = config.baseLevel;

        // distance: 75% from the world spawn + 25% from the nearest player's current respawn point.
        double worldDist = horizontalDistance(loc, loc.getWorld().getSpawnLocation());
        double respawnDist = worldDist;
        if (reference != null) {
            Location respawn = reference.getRespawnLocation();
            if (respawn != null && respawn.getWorld() == loc.getWorld()) {
                respawnDist = horizontalDistance(loc, respawn);
            }
        }
        danger += config.distanceFactor * (0.75 * worldDist + 0.25 * respawnDist);

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        long totalKills = 0;
        for (Player p : online) {
            totalKills += p.getStatistic(Statistic.MOB_KILLS);
        }
        danger += Math.floor(Math.cbrt((double) totalKills));

        int total = online.size();
        if (total > 0) {
            int netherVisitors = 0;
            int endVisitors = 0;
            for (Player p : online) {
                PlayerData pd = PlayerData.of(p);
                if (pd.hasVisitedNether()) {
                    netherVisitors++;
                }
                if (pd.hasVisitedEnd()) {
                    endVisitors++;
                }
            }
            danger += config.netherValue * ((double) netherVisitors / total);
            danger += config.endValue * ((double) endVisitors / total);
        }

        if (wobble && config.variation > 0) {
            danger += ThreadLocalRandom.current().nextGaussian() * config.variation;
        }
        return danger;
    }

    private double personalDanger(Location loc, Player reference) {
        if (reference == null) {
            return 0;
        }
        double pull = 0;
        for (Player p : loc.getWorld().getPlayers()) {
            double blockDist = Math.sqrt(sqDistXZ(loc, p.getLocation()));
            double chunkDist = Math.max(1.0, blockDist / CHUNK);
            pull += p.getStatistic(Statistic.MOB_KILLS) / Math.pow(chunkDist, GRAVITY_EXP);
        }
        if (pull <= 0) {
            return 0;
        }
        int k0 = reference.getStatistic(Statistic.MOB_KILLS);
        double importance = (double) k0 / (k0 + config.importanceHalfKills);
        return Math.cbrt(pull) * importance;
    }

    private double dayFactor() {
        long days = Bukkit.getWorlds().get(0).getGameTime() / 24000L;
        double raw = 1.0 + Math.sqrt((double) days) / 12.0;
        return Math.round(raw * 10.0) / 10.0; // round at the 2nd decimal -> 1 decimal place
    }

    public Player nearestPlayer(Location loc) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player p : loc.getWorld().getPlayers()) {
            double sq = sqDistXZ(loc, p.getLocation());
            if (sq < best) {
                best = sq;
                nearest = p;
            }
        }
        return nearest;
    }

    private static double horizontalDistance(Location a, Location b) {
        return Math.sqrt(sqDistXZ(a, b));
    }

    private static double sqDistXZ(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
