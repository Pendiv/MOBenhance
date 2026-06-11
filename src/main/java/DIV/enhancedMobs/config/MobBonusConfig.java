package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * config.yml の {@code mob-bonus} セクションから読み込むモブ種別ごとのレベルボーナス。
 *
 * <p>特性ロールより前に適用されるため、ボス強化は特性数・強度にも影響する。
 * 計算式: {@code round(level * (1 + multiply)) + add}。
 *
 * <p>キーはエンティティ種別の短縮名（例: {@code ender_dragon}、{@code wither}、{@code warden}）。
 * 注意:相対的なレベル上昇であるため。どういう状況であっても難易度上昇を確約する。やりすぎないように
 * 参考値は以下の通り
 * +100...ワールドレベルが1異なる。デバフエフェクトを扱う状態から不死が出現しうる
 * +50...おおよそ一般的に許容しうる限界。現行に多い特性の上位特性が出現しうる。がしかしぎりぎり対応が可能である
 * +20...通常のレベル付与の上振れでありうる範囲。そこまで気には留められないだろうが、ボスが下ぶれて楽勝になる、という事態には陥らないだろう
 */
public final class MobBonusConfig {

    private record Bonus(int add, double multiply) {
    }

    private final Map<String, Bonus> bonuses = new HashMap<>();

    public MobBonusConfig(EnhancedMobs plugin) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("mob-bonus");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            bonuses.put(key.toLowerCase(Locale.ROOT),
                    new Bonus(section.getInt("add", 0), section.getDouble("multiply", 0.0)));
        }
    }

    private String key(EntityType type) {
        return type.getKey().getKey();
    }

    /** ボーナス設定の有無（Monster 以外のモブをシステムに組み込む場合にも使用）。 */
    public boolean has(EntityType type) {
        return bonuses.containsKey(key(type));
    }

    /** ボーナス適用後のレベルを返す（設定がなければそのまま）。 */
    public int apply(EntityType type, int level) {
        Bonus bonus = bonuses.get(key(type));
        if (bonus == null) {
            return level;
        }
        return (int) Math.round(level * (1.0 + bonus.multiply())) + bonus.add();
    }
}
