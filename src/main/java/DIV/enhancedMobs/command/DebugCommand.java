package DIV.enhancedMobs.command;

import DIV.enhancedMobs.debug.DebugViewers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /emdebug} — プレイヤーごとのデバッグ表示をトグル。開発用足場。 */
public final class DebugCommand implements CommandExecutor {

    private final DebugViewers viewers;

    public DebugCommand(DebugViewers viewers) {
        this.viewers = viewers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enhancedmobs.debug")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can toggle the debug readout.", NamedTextColor.RED));
            return true;
        }
        boolean on = viewers.toggle(player.getUniqueId());
        player.sendMessage(Component.text(
                "Debug readout: " + (on ? "ON (hit a mob to see its stats)" : "OFF"),
                on ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        return true;
    }
}
