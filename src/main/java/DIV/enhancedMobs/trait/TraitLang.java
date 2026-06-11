package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * トレイトの日本語表示名と説明文を管理する。バンドルされた（管理者が編集可能な）
 * trait_names.yml / trait_desc.yml から読み込む。
 * 名前が未登録なら {@code shortName()} にフォールバック、説明が未登録なら固定の未登録マーカーを返す。
 */
public final class TraitLang {

    /** 内部 id と L2H の言語キーが異なるトレイトのエイリアスマップ。 */
    private static final Map<String, String> ALIAS = Map.of(
            "regen", "regenerate",
            "strike", "counter_strike",
            "ender", "teleport",
            "blind", "blindness",
            "confusion", "nausea");

    private static final String NO_DESC = "（説明未登録）";

    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> descs = new HashMap<>();

    public TraitLang(EnhancedMobs plugin) {
        load(plugin, "trait_names.yml", names);
        load(plugin, "trait_desc.yml", descs);
    }

    private void load(EnhancedMobs plugin, String resource, Map<String, String> into) {
        plugin.saveResource(resource, false);
        File file = new File(plugin.getDataFolder(), resource);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            into.put(key, yaml.getString(key));
        }
    }

    private String key(Trait trait) {
        return ALIAS.getOrDefault(trait.id(), trait.id());
    }

    public String name(Trait trait) {
        return names.getOrDefault(key(trait), trait.shortName());
    }

    public String desc(Trait trait) {
        return descs.getOrDefault(key(trait), NO_DESC);
    }
}
