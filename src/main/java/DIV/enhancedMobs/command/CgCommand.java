package DIV.enhancedMobs.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
 * 私がよく使っている易化コマンド
 * {@code /cg [target] [gamemode]} — ゲームモード変更コマンド。
 * <ul>
 *   <li>引数なし：自分のサバイバル↔クリエイティブをサイクル。</li>
 *   <li>{@code /cg <gamemode>}：自分のモードを指定。</li>
 *   <li>{@code /cg <target> <gamemode>}：対象プレイヤーのモードを指定。</li>
 * </ul>
 * ゲームモードは 0-3 の数字または名前で指定可能。権限: {@code enhancedmobs.cg}（OP）
 * ターゲットが指定されているなら、その後の省略は認められません
 */
public final class CgCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "enhancedmobs.cg";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a target and game mode.", NamedTextColor.RED));
                return true;
            }
            GameMode next = player.getGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
            player.setGameMode(next);
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a target and game mode.", NamedTextColor.RED));
                return true;
            }
            GameMode mode = parse(args[0]);
            if (mode == null) {
                sender.sendMessage(Component.text("Unknown game mode: " + args[0], NamedTextColor.RED));
                return true;
            }
            player.setGameMode(mode);
            return true;
        }

        GameMode mode = parse(args[1]);
        if (mode == null) {
            sender.sendMessage(Component.text("Unknown game mode: " + args[1], NamedTextColor.RED));
            return true;
        }
        List<Entity> selected;
        try {
            selected = Bukkit.selectEntities(sender, args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid target selector: " + args[0], NamedTextColor.RED));
            return true;
        }
        int count = 0;
        for (Entity entity : selected) {
            if (entity instanceof Player player) {
                player.setGameMode(mode);
                count++;
            }
        }
        sender.sendMessage(Component.text(
                "Set " + count + " player(s) to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
        return true;
    }

    private GameMode parse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(PERM)) {
            return out;
        }
        if (args.length == 1) {
            out.add("survival");
            out.add("creative");
            out.add("adventure");
            out.add("spectator");
            out.add("@a");
            out.add("@s");
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        } else if (args.length == 2) {
            out.add("survival");
            out.add("creative");
            out.add("adventure");
            out.add("spectator");
        }
        return out;
    }
}
