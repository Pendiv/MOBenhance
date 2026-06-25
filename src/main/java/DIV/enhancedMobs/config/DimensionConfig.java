package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ディメンション別の有効/無効切り替え・難易度スケーリング、および特性の無効化リスト。
 * dimensions.yml から読み込む。
 *
 * <p>スケーリング式は {@code level * multiply + add}。{@code default} ルールは
 * 未記載のディメンション（モッド追加ディメンション含む）に適用される。
 */
public final class DimensionConfig {

    private record Rule(boolean enabled, double add, double multiply) {
    }

    private final Map<String, Rule> rules = new HashMap<>();
    private final Set<String> disabledTraits = new HashSet<>();
    private Rule defaultRule = new Rule(true, 0.0, 1.0);

    /** レベル付与無効ディメンションでも常にレベル付与するモブの例外枠。 */
    private boolean levelingExceptionEnabled = true;
    private final Set<String> levelingExceptionMobs = new HashSet<>(List.of("wither", "warden"));

    public DimensionConfig(EnhancedMobs plugin) {
        File file = new File(plugin.getDataFolder(), "dimensions.yml");
        if (!file.exists()) {
            plugin.saveResource("dimensions.yml", false);
        }
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

        // 例外枠（既定: 有効・wither/warden）。dimensions.yml に節があれば上書き。
        ConfigurationSection ex = yaml.getConfigurationSection("leveling-exceptions");
        if (ex != null) {
            levelingExceptionEnabled = ex.getBoolean("enabled", true);
            if (ex.isList("mobs")) {
                levelingExceptionMobs.clear();
                for (String mob : ex.getStringList("mobs")) {
                    levelingExceptionMobs.add(mob.toLowerCase(Locale.ROOT));
                }
            }
        }
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

    /** ディメンションルールでレベルをスケーリング（{@code level * multiply + add}）。 */
    public int scaleLevel(World world, int level) {
        Rule rule = rule(world);
        return (int) Math.round(level * rule.multiply() + rule.add());
    }

    public boolean isTraitDisabled(String traitId) {
        return disabledTraits.contains(traitId);
    }

    /**
     * このモブ種別が例外枠か（レベル付与無効ディメンションでも常にレベルを付与する）。
     * 例外機能が無効なら常に false。
     */
    public boolean isLevelingException(EntityType type) {
        return levelingExceptionEnabled && levelingExceptionMobs.contains(type.getKey().getKey());
    }
}
