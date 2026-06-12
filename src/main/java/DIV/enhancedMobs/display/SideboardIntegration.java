package DIV.enhancedMobs.display;

import DIV.attributelib.api.Sideboard;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * attributelib のステータスサイドバー（/sideboard）への EnhancedMobs 統合。
 * <ul>
 *   <li>標準「ユーザー情報」のバニラ難易度行を削除し、EnhancedMobs の総合難易度
 *       （/difficultyset get と同じ値 = 揺らぎなし・本人基準・手動オフセット込み）に移し替える。</li>
 *   <li>「装備強化」ジャンルを追加: 手持ちの強化状態と、スキル名+クールタイム（0.1秒更新）。</li>
 * </ul>
 */
public final class SideboardIntegration {

    private SideboardIntegration() {
    }

    public static void init(EnhancedMobs plugin) {
        // バニラ難易度行 → EnhancedMobs の総合難易度へ移し替え。
        Sideboard.removeLine("user", "difficulty");
        Sideboard.addLine("user", Sideboard.line("difficulty",
                Component.text("総合難易度", NamedTextColor.WHITE),
                player -> Component.text(plugin.difficulty().playerDifficulty(player))));

        if (!plugin.mainConfig().enhancementEnabled) {
            return;
        }
        Sideboard.registerGenre(plugin, "enhance", Component.text("装備強化:", NamedTextColor.GOLD), List.of(
                Sideboard.line("held", Component.text("手持ち", NamedTextColor.YELLOW),
                        SideboardIntegration::heldLine),
                // CT は 0.1 秒単位で見たいので最短間隔（2tick）で更新する
                Sideboard.line("skill", Component.text("スキル", NamedTextColor.YELLOW),
                        SideboardIntegration::skillLine, 2)));
    }

    /** 手持ちの強化状態（Lv / XP / 精錬 / 破壊寸前）。 */
    private static Component heldLine(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemEnhancer.isEnhanceable(held) || held.getItemMeta() == null) {
            return Component.text("—", NamedTextColor.DARK_GRAY);
        }
        PersistentDataContainer pdc = held.getItemMeta().getPersistentDataContainer();
        int level = pdc.getOrDefault(ItemEnhancer.LEVEL, PersistentDataType.INTEGER, 1);
        int refine = pdc.getOrDefault(ItemEnhancer.REFINE, PersistentDataType.INTEGER, 0);
        String text = "Lv." + level + (refine > 0 ? " 精錬+" + refine : "");
        return ItemEnhancer.isBroken(held)
                ? Component.text(text + " 破壊寸前", NamedTextColor.RED)
                : Component.text(text);
    }

    /** 手持ちのレベリングスキルと残りクールタイム（バニラ Material CT 表示）。 */
    private static Component skillLine(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        String id = held.getItemMeta() == null ? null : ItemSkills.skillId(held);
        if (id == null) {
            return Component.text("—", NamedTextColor.DARK_GRAY);
        }
        int stage = ItemSkills.activeStage(held, id);
        String name = ItemSkills.skillDisplayName(id) + (stage > 0 ? " +" + stage : "");
        if (stage < 0) {
            return Component.text(name + "（Lv30で有効化）", NamedTextColor.DARK_GRAY);
        }
        int ct = player.getCooldown(held.getType());
        return ct > 0
                ? Component.text(String.format("%s CT %.1f秒", name, ct / 20.0), NamedTextColor.GRAY)
                : Component.text(name);
    }
}
