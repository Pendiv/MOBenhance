package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Per-dimension enable/disable + difficulty scaling, plus a global trait kill-switch list.
 * Loaded from dimensions.yml.
 *
 * <p>Scaling is {@code level * multiply + add}, so a rule can multiply, add, or both. The
 * {@code default} rule covers any dimension not listed (including unknown / modded ones).
 */
public final class DimensionConfig {

    private record Rule(boolean enabled, double add, double multiply) {
    }

    private final Map<String, Rule> rules = new HashMap<>();
    private final Set<String> disabledTraits = new HashSet<>();
    private Rule defaultRule = new Rule(true, 0.0, 1.0);

    public DimensionConfig(EnhancedMobs plugin) {
        plugin.saveResource("dimensions.yml", false);
        File file = new File(plugin.getDataFolder(), "dimensions.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection defaults = yaml.getConfigurationSection("default");
        if (defaults != null) {
            defaultRule = readRule(defaults, defaultRule);
        }
        ConfigurationSection dims = yaml.getConfigurationSection("dimensions");
        if (dims != null) {
            for (String key : dims.getKeys(false)) {
                ConfigurationSection section = dims.getConfigurationSection(key);
                if (section != null) {
                    rules.put(key.toLowerCase(Locale.ROOT), readRule(section, defaultRule));
                }
            }
        }
        disabledTraits.addAll(yaml.getStringList("disabled-traits"));
    }

    private Rule readRule(ConfigurationSection section, Rule fallback) {
        boolean enabled = section.getBoolean("enabled", fallback.enabled());
        double add = section.getDouble("add", fallback.add());
        double multiply = section.getDouble("multiply", fallback.multiply());
        return new Rule(enabled, add, multiply);
    }

    private Rule rule(World world) {
        return rules.getOrDefault(world.getKey().toString(), defaultRule);
    }

    public boolean isEnabled(World world) {
        return rule(world).enabled();
    }

    /** Scale a computed level by the dimension's rule: {@code level * multiply + add}. */
    public int scaleLevel(World world, int level) {
        Rule rule = rule(world);
        return (int) Math.round(level * rule.multiply() + rule.add());
    }

    public boolean isTraitDisabled(String traitId) {
        return disabledTraits.contains(traitId);
    }
}
