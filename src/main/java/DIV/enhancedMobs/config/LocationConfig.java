package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * location.yml から読み込む位置ベースの難易度倍率（バイオーム・深度）。
 * ディメンションスケーリングの後に乗算される。
 * 現世ではより深いほど強い、乗算であるため、理論上は2倍くらいになる...はずである
 */
public final class LocationConfig {

    private record Depth(int belowY, int step, double multiplier) {
    }

    private final Map<String, Double> biomeMultipliers = new HashMap<>();
    private final Map<String, Depth> depthByDimension = new HashMap<>();

    public LocationConfig(EnhancedMobs plugin) {
        File file = new File(plugin.getDataFolder(), "location.yml");
        if (!file.exists()) {
            plugin.saveResource("location.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection biomes = yaml.getConfigurationSection("biomes");
        if (biomes != null) {
            for (String key : biomes.getKeys(false)) {
                biomeMultipliers.put(key.toLowerCase(Locale.ROOT), biomes.getDouble(key, 1.0));
            }
        }
        ConfigurationSection depth = yaml.getConfigurationSection("depth");
        if (depth != null) {
            for (String key : depth.getKeys(false)) {
                ConfigurationSection s = depth.getConfigurationSection(key);
                if (s != null) {
                    depthByDimension.put(key.toLowerCase(Locale.ROOT),
                            new Depth(s.getInt("below-y", 0), Math.max(1, s.getInt("step", 5)),
                                    s.getDouble("multiplier", 1.0)));
                }
            }
        }
    }

    /** バイオーム倍率 × 深度倍率を返す。 */
    public double multiplier(Location loc) {
        return biomeMultiplier(loc) * depthMultiplier(loc);
    }

    private double biomeMultiplier(Location loc) {
        String key = loc.getBlock().getBiome().getKey().toString();
        return biomeMultipliers.getOrDefault(key, 1.0);
    }

    private double depthMultiplier(Location loc) {
        Depth depth = depthByDimension.get(loc.getWorld().getKey().toString());
        if (depth == null || loc.getY() >= depth.belowY()) {
            return 1.0;
        }
        int below = (int) (depth.belowY() - loc.getY());
        int steps = below / depth.step();
        return Math.pow(depth.multiplier(), steps);
    }
}
