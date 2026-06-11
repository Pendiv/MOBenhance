package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Japanese display names for traits, loaded from the bundled (and admin-editable) trait_names.yml.
 * Falls back to the trait's English {@code shortName()} if no translation is present.
 */
public final class TraitLang {

    /** Trait ids whose translation key differs from our id (L2H lang uses other names). */
    private static final Map<String, String> ALIAS = Map.of(
            "regen", "regenerate",
            "strike", "counter_strike",
            "ender", "teleport",
            "blind", "blindness",
            "confusion", "nausea");

    private final Map<String, String> names = new HashMap<>();

    public TraitLang(EnhancedMobs plugin) {
        plugin.saveResource("trait_names.yml", false);
        File file = new File(plugin.getDataFolder(), "trait_names.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            names.put(key, yaml.getString(key));
        }
    }

    public String name(Trait trait) {
        String key = ALIAS.getOrDefault(trait.id(), trait.id());
        return names.getOrDefault(key, trait.shortName());
    }
}
