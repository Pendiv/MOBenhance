package DIV.enhancedMobs.command;

import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.world.AmeijiaGate;
import net.kyori.adventure.text.Component;
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
            Lang.send(sender, "emob.common.no_perm");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "load" -> load(sender);
            case "tp" -> tp(sender);
            case "gate" -> gate(sender);
            default -> Lang.send(sender, "emob.dimension.usage", Component.text(label));
        }
        return true;
    }

    /** 視線の先のブロックを END_PORTAL（エンドポータルのテクスチャ）にして、アメイジア行きゲートにする。 */
    private void gate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "emob.common.player_only");
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            Lang.send(sender, "emob.dimension.aim_block");
            return;
        }
        AmeijiaGate.place(target);
        Lang.send(sender, "emob.dimension.gate_placed", Component.text(target.getX()),
                Component.text(target.getY()), Component.text(target.getZ()));
    }

    private void load(CommandSender sender) {
        if (plugin.getServer().getWorld(AMEIJIA) != null) {
            Lang.send(sender, "emob.dimension.already_loaded");
            return;
        }
        Lang.send(sender, "emob.dimension.loading");
        try {
            World world = new WorldCreator("ameijia", AMEIJIA).createWorld();
            if (world != null) {
                Lang.send(sender, "emob.dimension.load_ok", Component.text(world.getKey().toString()),
                        Component.text(world.getEnvironment().name()));
            } else {
                Lang.send(sender, "emob.dimension.load_null");
            }
        } catch (Exception e) {
            Lang.send(sender, "emob.dimension.load_err", Component.text(String.valueOf(e.getMessage())));
            plugin.getLogger().warning("ameijia load failed: " + e);
        }
    }

    private void tp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "emob.common.player_only");
            return;
        }
        World world = plugin.getServer().getWorld(AMEIJIA);
        if (world == null) {
            Lang.send(sender, "emob.dimension.not_loaded");
            return;
        }
        Location spawn = world.getSpawnLocation();
        player.teleport(spawn);
        Lang.send(sender, "emob.dimension.tp_ok");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERM)) {
            return List.of("load", "tp", "gate");
        }
        return List.of();
    }
}
