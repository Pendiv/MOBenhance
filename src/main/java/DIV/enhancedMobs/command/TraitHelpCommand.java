package DIV.enhancedMobs.command;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitLang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * ヘルプコマンド
 * {@code /traithelp [sort [min N] [max N] | <id|"日本語名">]} — 特性一覧・検索コマンド。
 *
 * <p>権限: {@code enhancedmobs.traithelp}（全員に開放）。
 * <ul>
 *   <li>引数なし：出現レベル順に全特性を一覧表示。</li>
 *   <li>{@code sort min 200 max 300}：出現レベル範囲でフィルタ。</li>
 *   <li>{@code <id>} または {@code "日本語名"}：その特性の詳細を表示。</li>
 * </ul>
 */
public final class TraitHelpCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "enhancedmobs.traithelp";

    private final EnhancedMobs plugin;

    public TraitHelpCommand(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendList(sender, allSortedByLevel(), Integer.MIN_VALUE, Integer.MAX_VALUE);
            return true;
        }
        if (args[0].equalsIgnoreCase("sort")) {
            handleSort(sender, args);
            return true;
        }
        handleLookup(sender, args);
        return true;
    }

    // ---- ソート・フィルタ ----

    private void handleSort(CommandSender sender, String[] args) {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        for (int i = 1; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            boolean isMin = token.equals("min");
            boolean isMax = token.equals("max");
            if ((isMin || isMax) && i + 1 < args.length) {
                try {
                    int value = Integer.parseInt(args[++i]);
                    if (isMin) {
                        min = value;
                    } else {
                        max = value;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text(token + " の値が数値ではありません: " + args[i], NamedTextColor.RED));
                    return;
                }
            }
        }
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        List<Trait> filtered = new ArrayList<>();
        for (Trait trait : allSortedByLevel()) {
            if (trait.minLevel() >= min && trait.minLevel() <= max) {
                filtered.add(trait);
            }
        }
        sendList(sender, filtered, min, max);
    }

    // ---- 単一特性の検索 ----

    private void handleLookup(CommandSender sender, String[] args) {
        String query = stripQuotes(String.join(" ", args)).trim();
        TraitLang lang = plugin.traits().lang();

        // 1) ID完全一致。
        Trait byId = plugin.traits().registry().byId(query.toLowerCase(Locale.ROOT));
        if (byId != null) {
            sendDetail(sender, byId, lang);
            return;
        }

        // 2) 日本語名の完全一致、次いで部分一致。
        Trait exact = null;
        List<Trait> partial = new ArrayList<>();
        for (Trait trait : plugin.traits().registry().all()) {
            String name = lang.name(trait);
            if (name.equals(query)) {
                exact = trait;
                break;
            }
            if (name.contains(query)) {
                partial.add(trait);
            }
        }
        if (exact != null) {
            sendDetail(sender, exact, lang);
            return;
        }
        if (partial.size() == 1) {
            sendDetail(sender, partial.get(0), lang);
            return;
        }
        if (partial.size() > 1) {
            Component msg = Component.text("「" + query + "」に一致する特性が複数あります:", NamedTextColor.YELLOW);
            for (Trait trait : partial) {
                msg = msg.append(Component.newline())
                        .append(Component.text("  " + lang.name(trait), NamedTextColor.AQUA))
                        .append(Component.text(" (" + trait.id() + ")", NamedTextColor.GRAY));
            }
            sender.sendMessage(msg);
            return;
        }
        sender.sendMessage(Component.text("特性が見つかりません: " + query, NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("一覧は /traithelp で確認できます。", NamedTextColor.GRAY)));
    }

    private void sendDetail(CommandSender sender, Trait trait, TraitLang lang) {
        Component msg = Component.text("【" + lang.name(trait) + "】", NamedTextColor.GOLD)
                .append(Component.text(" (" + trait.id() + ")", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("出現Lv: " + trait.minLevel(), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text(lang.desc(trait), NamedTextColor.WHITE));
        sender.sendMessage(msg);
    }

    // ---- 一覧表示 ----

    private void sendList(CommandSender sender, List<Trait> traits, int min, int max) {
        TraitLang lang = plugin.traits().lang();
        Component header;
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            header = Component.text("特性一覧 (" + traits.size() + "件)", NamedTextColor.GOLD);
        } else {
            String lo = min == Integer.MIN_VALUE ? "0" : Integer.toString(min);
            String hi = max == Integer.MAX_VALUE ? "∞" : Integer.toString(max);
            header = Component.text("特性一覧 出現Lv " + lo + "〜" + hi
                    + " (" + traits.size() + "件)", NamedTextColor.GOLD);
        }
        Component msg = header;
        for (Trait trait : traits) {
            msg = msg.append(Component.newline())
                    .append(Component.text(lang.name(trait), NamedTextColor.AQUA))
                    .append(Component.text(" (Lv." + trait.minLevel() + ")", NamedTextColor.GRAY));
        }
        if (traits.isEmpty()) {
            msg = msg.append(Component.newline())
                    .append(Component.text("該当する特性はありません。", NamedTextColor.GRAY));
        }
        sender.sendMessage(msg);
    }

    private List<Trait> allSortedByLevel() {
        List<Trait> list = new ArrayList<>(plugin.traits().registry().all());
        TraitLang lang = plugin.traits().lang();
        list.sort(Comparator.comparingInt(Trait::minLevel).thenComparing(lang::name));
        return list;
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(PERM)) {
            return out;
        }
        if (args.length == 1) {
            if ("sort".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                out.add("sort");
            }
            // 直接検索用にトレイトIDも補完候補として追加。
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Trait trait : plugin.traits().registry().all()) {
                if (trait.id().startsWith(prefix)) {
                    out.add(trait.id());
                }
            }
        } else if (args[0].equalsIgnoreCase("sort")) {
            String last = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (String kw : List.of("min", "max")) {
                if (kw.startsWith(last)) {
                    out.add(kw);
                }
            }
        }
        return out;
    }
}
