package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-entity-type trait control loaded from entities.yml — the plugin analogue of L2Hostility's
 * EntityConfig whitelist/blacklist.
 */
public final class EntityConfig {

    private record Rule(Set<String> whitelist, Set<String> blacklist) {
    }

    private final Map<String, Rule> rules = new HashMap<>();

    public EntityConfig(EnhancedMobs plugin) {
        File file = new File(plugin.getDataFolder(), "entities.yml");
        if (!file.exists()) {
            plugin.saveResource("entities.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("entities");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Set<String> whitelist = new HashSet<>(section.getStringList("whitelist"));
            Set<String> blacklist = new HashSet<>(section.getStringList("blacklist"));
            rules.put(key.toLowerCase(), new Rule(whitelist, blacklist));
        }
    }

    /** Whether the given trait id may roll on the given entity type (entity-side restriction). */
    public boolean allows(EntityType type, String traitId) {
        Rule rule = rules.get(type.getKey().toString());
        if (rule == null) {
            return true;
        }
        if (!rule.whitelist().isEmpty() && !rule.whitelist().contains(traitId)) {
            return false;
        }
        return !rule.blacklist().contains(traitId);
    }
}
