package DIV.enhancedMobs.command;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.debug.DebugViewers;
import DIV.enhancedMobs.i18n.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EnhancedMobs の統括コマンド {@code /emob <サブ> ...}。各機能を1コマンドに集約する。
 * 個々のサブコマンドは従来の Executor をそのまま委譲して動かす（権限も各 Executor が自前で検査）。
 * skill だけは Executor 側に権限検査が無いため、ここで {@code enhancedmobs.levelingskill} を確認する。
 */
public final class EmobCommand implements CommandExecutor, TabCompleter {

    /** サブコマンド定義: 権限（不要なら null = Executor が自前検査）・実行・補完（無ければ null）。 */
    private record Sub(String permission, CommandExecutor executor, TabCompleter tab, String usage) {
    }

    private final Map<String, Sub> subs = new LinkedHashMap<>();

    public EmobCommand(EnhancedMobs plugin, DebugViewers debugViewers) {
        DifficultyCommand difficulty = new DifficultyCommand(plugin);
        CgCommand gamemode = new CgCommand();
        TraitHelpCommand trait = new TraitHelpCommand(plugin);
        LevelingSkillCommand skill = new LevelingSkillCommand();
        DebugCommand debug = new DebugCommand(debugViewers);
        DimensionCommand dimension = new DimensionCommand(plugin);
        subs.put("difficulty", new Sub(null, difficulty, difficulty, "<get|set|add|reset> [対象] [値]"));
        subs.put("gamemode", new Sub(null, gamemode, gamemode, "[対象] [ゲームモード]"));
        subs.put("trait", new Sub(null, trait, trait, "[sort [min N] [max N] | <id|\"名前\">]"));
        subs.put("skill", new Sub("enhancedmobs.levelingskill", skill, skill, "<対象> <lvl|skill|skill_lvl> <値>"));
        subs.put("dimension", new Sub("enhancedmobs.dimension", dimension, dimension, "<load|tp|gate>"));
        subs.put("debug", new Sub(null, debug, null, "（被弾時のデバッグ表示を切替）"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        Sub sub = subs.get(name);
        if (sub == null) {
            Lang.send(sender, "emob.cmd.unknown_sub", Component.text(args[0]));
            sendHelp(sender, label);
            return true;
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            Lang.send(sender, "emob.common.no_perm");
            return true;
        }
        // 委譲時の label を "emob <サブ>" にして、各 Executor の使い方表示を正しくする。
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return sub.executor().onCommand(sender, command, label + " " + name, rest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, Sub> e : subs.entrySet()) {
                Sub sub = e.getValue();
                boolean allowed = sub.permission() == null || sender.hasPermission(sub.permission());
                if (allowed && e.getKey().startsWith(prefix)) {
                    out.add(e.getKey());
                }
            }
            return out;
        }
        Sub sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || sub.tab() == null
                || (sub.permission() != null && !sender.hasPermission(sub.permission()))) {
            return List.of();
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return sub.tab().onTabComplete(sender, command, alias, rest);
    }

    private void sendHelp(CommandSender sender, String label) {
        Lang.send(sender, "emob.cmd.help_header");
        subs.forEach((name, sub) ->
                sender.sendMessage(Component.text("  /" + label + " " + name + " " + sub.usage(), NamedTextColor.GRAY)));
    }
}
