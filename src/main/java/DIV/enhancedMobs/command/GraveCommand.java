package DIV.enhancedMobs.command;

import DIV.enhancedMobs.grave.Grave;
import DIV.enhancedMobs.grave.GraveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /grave} — 自分の墓の一覧（座標案内）と、近接（顕現範囲内）での一括回収。
 * 遠隔回収は無し（詰みは管理者対応）。
 */
public final class GraveCommand implements CommandExecutor, TabCompleter {

    private final GraveManager manager;

    public GraveCommand(GraveManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤー専用コマンドです。");
            return true;
        }
        if (!manager.isCommandEnabled()) {
            player.sendMessage(Component.text("/grave コマンドは無効化されています。", NamedTextColor.RED));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("recover")) {
            int n = manager.recoverNearby(player);
            player.sendMessage(n > 0
                    ? Component.text(n + " 個の墓から回収しました。", NamedTextColor.GREEN)
                    : Component.text("回収できる墓が近く（顕現範囲内）にありません。", NamedTextColor.YELLOW));
            return true;
        }
        list(player);
        return true;
    }

    private void list(Player player) {
        List<Grave> graves = manager.gravesOf(player);
        if (graves.isEmpty()) {
            player.sendMessage(Component.text("あなたの墓はありません。", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("=== あなたの墓 (" + graves.size() + ") ===", NamedTextColor.LIGHT_PURPLE));
        long now = System.currentTimeMillis();
        for (Grave g : graves) {
            long ageHours = Math.max(0, (now - g.createdMillis()) / (60L * 60L * 1000L));
            String age = ageHours >= 24 ? (ageHours / 24) + "日" + (ageHours % 24) + "時間前" : ageHours + "時間前";
            int dist = manager.distanceTo(player, g);
            String distStr = dist < 0 ? "別ワールド" : dist + "m";
            player.sendMessage(Component.text(
                    "・" + g.worldName() + " (" + g.x() + ", " + g.y() + ", " + g.z() + ")  距離" + distStr
                            + "  " + age + (g.isMaterialized() ? "  [顕現中]" : "  [未顕現]"),
                    NamedTextColor.AQUA));
        }
        player.sendMessage(Component.text("墓の32ブロック以内に行くとチェストが顕現します。/grave recover で一括回収も可。", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? List.of("recover", "list") : List.of();
    }
}
