package DIV.enhancedMobs.core;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * エンティティに付けるタグ（PDC にカンマ区切りで保存）。
 * 同種モブ同士が特性で連携する「ファミリー」判定に使用。
 * 特性の時空タイプ群が主に使用
 */
public final class MobTags {

    private static final NamespacedKey KEY = new NamespacedKey(EnhancedMobs.get(), "tags");

    private MobTags() {
    }

    public static void add(PersistentDataHolder holder, String tag) {
        Set<String> tags = get(holder);
        if (tags.add(tag)) {
            holder.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, String.join(",", tags));
        }
    }

    public static boolean has(PersistentDataHolder holder, String tag) {
        return get(holder).contains(tag);
    }

    private static Set<String> get(PersistentDataHolder holder) {
        String raw = holder.getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.STRING, "");
        Set<String> set = new LinkedHashSet<>();
        if (!raw.isEmpty()) {
            for (String tag : raw.split(",")) {
                if (!tag.isEmpty()) {
                    set.add(tag);
                }
            }
        }
        return set;
    }
}
