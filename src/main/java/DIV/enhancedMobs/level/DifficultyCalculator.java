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
 * 多層的な「危険度」モデルからモブのレベルを算出する。
 *
 * <pre>
 * danger = (D_global + D_personal) * T + offset(reference)      [モブはクランプあり]
 *
 * D_global   = base
 *            + distanceFactor * ワールドスポーンからの距離
 *            + floor( cbrt( 全オンラインプレイヤーの MOB_KILLS 合計 ) )
 *            + netherValue * (ネザー訪問者数 / 全オンライン数)
 *            + endValue    * (エンド訪問者数  / 全オンライン数)
 *            + gaussian * variation                              (モブスポーン時のみ)
 *
 * D_personal = cbrt( sum_i  X_i / N_i^1.6 ) * I                 (重力 + 重要度)
 *   X_i = プレイヤー i の MOB_KILLS、N_i = プレイヤー i → モブのチャンク距離 (最小1)、
 *   対象はモブと同じワールドのプレイヤーのみ。
 *   I   = K0 / (K0 + c)、K0 = 基準プレイヤーの MOB_KILLS。
 *
 * T          = round1( 1 + sqrt(Y) / 12 )、Y = プライマリワールドの経過日数。
 * offset     = 基準プレイヤーの手動難易度調整値（OP が設定）。
 * </pre>
 *
 * <p>スポーン時の基準は最近傍プレイヤー、プレイヤー自身の表示時は本人。
 * 双方で同じメソッドを使うため、プレイヤーが見る数値とモブに適用される数値が一致する。
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

    /** スポーン時のレベルを算出（揺らぎあり）。[0, maxMobLevel] にクランプ。 */
    public int compute(Location loc) {
        Player reference = nearestPlayer(loc);
        int level = (int) Math.round(rawDanger(loc, reference, true));
        level = dimensions.scaleLevel(loc.getWorld(), level);
        level = (int) Math.round(level * locations.multiplier(loc));
        level = (int) Math.round(level * config.finalMultiplier);
        level = (int) Math.round(applyAccidentRelief(level, reference)); // 連続死亡による事故救済
        if (level < 0) {
            level = 0;
        }
        if (level > config.maxMobLevel) {
            level = config.maxMobLevel;
        }
        return level;
    }

    /** プレイヤーの難易度表示値を算出（揺らぎなし）。 */
    public int playerDifficulty(Player player) {
        int value = (int) Math.round(rawDanger(player.getLocation(), player, false));
        value = dimensions.scaleLevel(player.getWorld(), value);
        value = (int) Math.round(value * locations.multiplier(player.getLocation()));
        value = (int) Math.round(value * config.finalMultiplier);
        value = (int) Math.round(applyAccidentRelief(value, player)); // 連続死亡による事故救済（本人）
        return Math.max(0, value);
    }

    /**
     * 事故救済: 基準プレイヤーの連続死亡に応じて危険度を下げる。
     * {@code value × (1 - 永続軽減) × (1 - 臨時軽減) − 臨時実数軽減}。臨時分は MC1日で失効する。
     */
    private double applyAccidentRelief(double value, Player reference) {
        if (reference == null) {
            return value;
        }
        PlayerData pd = PlayerData.of(reference);
        long day = Bukkit.getWorlds().get(0).getGameTime() / 24000L;
        double mult = (1.0 - pd.reliefPermanentPct()) * (1.0 - pd.reliefTempPct(day));
        return value * mult - pd.reliefTempFlat(day);
    }

    private double rawDanger(Location loc, Player reference, boolean wobble) {
        double core = (globalDanger(loc, reference, wobble) + personalDanger(loc, reference)) * dayFactor();
        double offset = reference == null ? 0.0 : PlayerData.of(reference).getDifficultyOffset();
        return core + offset;
    }

    private double globalDanger(Location loc, Player reference, boolean wobble) {
        double danger = config.baseLevel;

        // 距離: ワールドスポーン 75% + 基準プレイヤーのリスポーン地点 25% で加重平均。
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
        return Math.round(raw * 10.0) / 10.0; // 小数点第2位で四捨五入 → 小数1桁
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
