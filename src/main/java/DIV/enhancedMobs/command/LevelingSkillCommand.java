package DIV.enhancedMobs.command;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OP 用デバッグコマンド。手持ち（メインハンド）の強化対象アイテムを直接操作する。
 * <pre>
 * /levelingskill &lt;セレクタ&gt; lvl &lt;1..100&gt;       — 強化レベルを設定（cap で切る）
 * /levelingskill &lt;セレクタ&gt; skill &lt;id|none&gt;   — レベリングスキルを設定/削除
 * /levelingskill &lt;セレクタ&gt; skill_lvl &lt;0..3&gt;  — スキル強化段階を設定（レベルを 30/50/70/100 に変更）
 * </pre>
 */
public final class LevelingSkillCommand implements CommandExecutor, TabCompleter {

    /** 強化段階 0〜3 に対応するアイテムレベル。 */
    private static final int[] STAGE_LEVELS = {30, 50, 70, 100};

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            msg(sender, "使い方: /" + label + " <セレクタ> <lvl|skill|skill_lvl> <値>", NamedTextColor.RED);
            return true;
        }
        List<Entity> targets;
        try {
            targets = Bukkit.selectEntities(sender, args[0]);
        } catch (IllegalArgumentException ex) {
            msg(sender, "セレクタが不正です: " + args[0], NamedTextColor.RED);
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        String value = args[2];
        int applied = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!ItemEnhancer.isEnhanceable(item)) {
                msg(sender, player.getName() + ": 手持ちが強化対象ではありません", NamedTextColor.GRAY);
                continue;
            }
            boolean ok;
            try {
                ok = switch (mode) {
                    case "lvl" -> ItemEnhancer.setLevel(item, Integer.parseInt(value));
                    case "skill" -> ItemSkills.setSkill(item, value.toLowerCase(Locale.ROOT));
                    case "skill_lvl" -> {
                        int stage = Integer.parseInt(value);
                        yield stage >= 0 && stage <= 3 && ItemEnhancer.setLevel(item, STAGE_LEVELS[stage]);
                    }
                    default -> {
                        msg(sender, "不明なモード: " + mode + "（lvl / skill / skill_lvl）", NamedTextColor.RED);
                        yield false;
                    }
                };
            } catch (NumberFormatException ex) {
                msg(sender, "数値が不正です: " + value, NamedTextColor.RED);
                return true;
            }
            if (ok) {
                player.getInventory().setItemInMainHand(item);
                applied++;
            }
        }
        msg(sender, applied + " 人に適用しました", applied > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(List.of("@s", "@p", "@a"));
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            return filter(out, args[0]);
        }
        if (args.length == 2) {
            return filter(List.of("lvl", "skill", "skill_lvl"), args[1]);
        }
        if (args.length == 3) {
            return switch (args[1].toLowerCase(Locale.ROOT)) {
                case "lvl" -> filter(List.of("1", "10", "30", "50", "70", "100"), args[2]);
                case "skill" -> {
                    List<String> ids = new ArrayList<>(ItemSkills.SKILL_IDS);
                    ids.add("none");
                    yield filter(ids, args[2]);
                }
                case "skill_lvl" -> filter(List.of("0", "1", "2", "3"), args[2]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private static void msg(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
