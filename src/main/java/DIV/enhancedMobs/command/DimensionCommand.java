package DIV.enhancedMobs.command;

import DIV.enhancedMobs.world.AmeijiaGate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/**
 * カスタムディメンションの読み込み・移動（OP）。
 * データパックで定義したディメンションを {@link WorldCreator} でロードできるか試すためのもの。
 *
 * <pre>
 * /emob dimension load   - データパックのディメンション(enhancedmobs:ameijia)をワールドとしてロード
 * /emob dimension tp     - ロード済みディメンションへ自分をテレポート
 * </pre>
 */
public final class DimensionCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "enhancedmobs.dimension";
    /** 試験対象ディメンション（データパックの dimension 定義のキー）。 */
    private static final NamespacedKey AMEIJIA = AmeijiaGate.WORLD_KEY;

    private final Plugin plugin;

    public DimensionCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "load" -> load(sender);
            case "tp" -> tp(sender);
            case "gate" -> gate(sender);
            default -> sender.sendMessage(Component.text("使い方: /" + label + " <load|tp|gate>", NamedTextColor.YELLOW));
        }
        return true;
    }

    /** 視線の先のブロックを END_PORTAL（エンドポータルのテクスチャ）にして、アメイジア行きゲートにする。 */
    private void gate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行できます。", NamedTextColor.RED));
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            sender.sendMessage(Component.text("6ブロック以内のブロックに照準を合わせてください。", NamedTextColor.YELLOW));
            return;
        }
        AmeijiaGate.place(target);
        sender.sendMessage(Component.text("ゲートを召喚しました（" + target.getX() + "," + target.getY() + ","
                + target.getZ() + "）。入るとアメイジアへ。", NamedTextColor.GREEN));
    }

    private void load(CommandSender sender) {
        if (plugin.getServer().getWorld(AMEIJIA) != null) {
            sender.sendMessage(Component.text("既にロード済みです。", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("ameijia をロード中…（初回は地形生成に時間がかかります）", NamedTextColor.GRAY));
        try {
            World world = new WorldCreator("ameijia", AMEIJIA).createWorld();
            if (world != null) {
                sender.sendMessage(Component.text("ロード成功: " + world.getKey()
                        + "（環境 " + world.getEnvironment() + "）", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text(
                        "ロード失敗（null）。データパック未読込の可能性 → サーバー再起動が必要かも。",
                        NamedTextColor.RED));
            }
        } catch (Exception e) {
            sender.sendMessage(Component.text("ロード失敗: " + e.getMessage()
                    + "（データパック配備直後は再起動が必要）", NamedTextColor.RED));
            plugin.getLogger().warning("ameijia load failed: " + e);
        }
    }

    private void tp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行できます。", NamedTextColor.RED));
            return;
        }
        World world = plugin.getServer().getWorld(AMEIJIA);
        if (world == null) {
            sender.sendMessage(Component.text("まだロードされていません（/emob dimension load を先に）。", NamedTextColor.RED));
            return;
        }
        Location spawn = world.getSpawnLocation();
        player.teleport(spawn);
        sender.sendMessage(Component.text("ameijia へ移動しました。", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERM)) {
            return List.of("load", "tp", "gate");
        }
        return List.of();
    }
}
