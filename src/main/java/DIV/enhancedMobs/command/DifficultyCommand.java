package DIV.enhancedMobs.command;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 難易度設定用コマンド兼難易度確認コマンド
 * {@code /difficultyset <get|set|add|reset> ...}
 *
 * <p>権限: {@code get} は全員（{@code enhancedmobs.difficulty.get}）、
 * ->発動したプレイヤーの現行の最終難易度を出力。それだけ
 * {@code set/add/reset} はOP用（{@code enhancedmobs.difficulty.manage}）。
 * ->難易度を変化/リセットさせる。こっちはOP用
 */
public final class DifficultyCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_GET = "enhancedmobs.difficulty.get";
    private static final String PERM_MANAGE = "enhancedmobs.difficulty.manage";

    private final EnhancedMobs plugin;

    public DifficultyCommand(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /difficultyset <get|set|add|reset> ...", NamedTextColor.YELLOW));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "get" -> handleGet(sender);
            case "set", "add", "reset" -> handleManage(sender, sub, args);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleGet(CommandSender sender) {
        if (!sender.hasPermission(PERM_GET)) {
            deny(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /difficulty get.", NamedTextColor.RED));
            return true;
        }
        PlayerData data = PlayerData.of(player);
        int difficulty = plugin.difficulty().playerDifficulty(player);
        double offset = data.getDifficultyOffset();
        Component msg = Component.text("Your difficulty: " + difficulty, NamedTextColor.AQUA);
        if (offset != 0) {
            msg = msg.append(Component.text(" (offset " + fmt(offset) + ")", NamedTextColor.GRAY));
        }
        player.sendMessage(msg);
        return true;
    }

    private boolean handleManage(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission(PERM_MANAGE)) {
            deny(sender);
            return true;
        }
        boolean needsCount = !sub.equals("reset");
        if (args.length < 2 || (needsCount && args.length < 3)) {
            sender.sendMessage(Component.text(
                    "Usage: /difficultyset " + sub + " <target>" + (needsCount ? " <count>" : ""),
                    NamedTextColor.YELLOW));
            return true;
        }

        List<Entity> selected;
        try {
            selected = Bukkit.selectEntities(sender, args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid target selector: " + args[1], NamedTextColor.RED));
            return true;
        }

        double count = 0;
        if (needsCount) {
            try {
                count = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Count must be a number: " + args[2], NamedTextColor.RED));
                return true;
            }
        }

        int affected = 0;
        for (Entity entity : selected) {
            if (!(entity instanceof Player target)) {
                continue;
            }
            PlayerData data = PlayerData.of(target);
            switch (sub) {
                case "add" -> data.setDifficultyOffset(data.getDifficultyOffset() + count);
                case "set" -> data.setDifficultyOffset(count);
                case "reset" -> data.setDifficultyOffset(0);
                default -> {
                }
            }
            affected++;
        }

        sender.sendMessage(Component.text(
                "difficulty " + sub + " applied to " + affected + " player(s).", NamedTextColor.GREEN));
        return true;
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
    }

    private String fmt(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("get", "set", "add", "reset")) {
                boolean allowed = sub.equals("get") ? sender.hasPermission(PERM_GET) : sender.hasPermission(PERM_MANAGE);
                if (allowed && sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("get") && sender.hasPermission(PERM_MANAGE)) {
            out.add("@a");
            out.add("@p");
            out.add("@s");
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }
}
