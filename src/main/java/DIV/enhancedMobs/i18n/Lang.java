package DIV.enhancedMobs.i18n;

import DIV.enhancedMobs.EnhancedMobs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 表示テキストの一元管理 / 多言語対応。
 *
 * <p>翻訳キー（例 {@code "emob.break.cleared"}）を {@link Component#translatable} として投げ、
 * Adventure の {@link GlobalTranslator} に登録した辞書ストアが、送信時に各プレイヤーの
 * クライアント言語（{@code player.locale()}）へ自動でレンダリングする。リソースパックは不要。</p>
 *
 * <p>辞書は MiniMessage 文字列で、装飾・色（{@code <red>...}）と引数（{@code <argument:0>}）を
 * 含められる。バンドルされた {@code lang/ja_jp.yml} / {@code lang/en_us.yml} を管理者が編集可能な
 * 形でデータフォルダへ展開して読み込む。既定ロケールは日本語。</p>
 *
 * <p>アイテムの lore / 名前などクライアント側のキー解決に頼れない箇所は {@link #render} で
 * サーバ表示ロケール（既定 ja）に確定レンダリングして使う。</p>
 */
public final class Lang {

    private static final Key STORE_KEY = Key.key("enhancedmobs", "lang");
    /** 既定ロケール（辞書に該当ロケールが無い時のフォールバック先）。 */
    private static final Locale DEFAULT_LOCALE = Locale.JAPANESE;

    /** lore 等、クライアント解決に頼れない箇所を確定レンダリングするためのサーバ表示ロケール。 */
    private static Locale displayLocale = DEFAULT_LOCALE;
    private static MiniMessageTranslationStore store;

    private Lang() {
    }

    public static void init(EnhancedMobs plugin) {
        store = MiniMessageTranslationStore.create(STORE_KEY);
        store.defaultLocale(DEFAULT_LOCALE);
        load(plugin, "lang/ja_jp.yml", Locale.JAPANESE);
        load(plugin, "lang/en_us.yml", Locale.ENGLISH);
        GlobalTranslator.translator().addSource(store);
    }

    private static void load(EnhancedMobs plugin, String resourcePath, Locale locale) {
        plugin.saveResource(resourcePath, false); // データフォルダへ展開（既存は上書きしない=管理者編集を尊重）
        // 同梱既定（jar 内）を土台に、ディスク版（管理者編集）で上書きする。
        // こうすると新しく追加したキーは常に同梱既定から解決でき、既存キーの編集も尊重される
        // （saveResource(false) は既存を上書きしないため、追加キーがディスクに無い問題への対処）。
        YamlConfiguration bundled = bundledDefaults(plugin, resourcePath);
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), resourcePath));
        Set<String> keys = new LinkedHashSet<>();
        if (bundled != null) {
            keys.addAll(bundled.getKeys(true));
        }
        keys.addAll(disk.getKeys(true));
        int count = 0;
        int filled = 0;
        for (String key : keys) {
            String value = disk.isString(key) ? disk.getString(key)
                    : (bundled != null && bundled.isString(key) ? bundled.getString(key) : null);
            if (value != null) {
                store.register(key, locale, value);
                count++;
                if (!disk.isString(key)) {
                    filled++; // ディスクに無く同梱既定で補完したキー
                }
            }
        }
        plugin.getLogger().info("言語ファイル " + resourcePath + " を読み込み（" + count + " キー"
                + (filled > 0 ? "、うち " + filled + " 件は同梱既定で補完" : "") + "）");
    }

    /** jar 同梱の既定言語ファイル（全キーの土台）。取得不可なら null。 */
    private static YamlConfiguration bundledDefaults(EnhancedMobs plugin, String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    // ---- キー → Component ----

    /**
     * 翻訳キーの Component（各クライアント言語でレンダリングされる。チャット/アクションバー等の
     * メッセージ向け）。引数は MiniMessage 側で {@code <argument:0>} 等として参照される。
     */
    public static Component of(String key, ComponentLike... args) {
        if (args.length == 0) {
            return Component.translatable(key);
        }
        return Component.translatable().key(key).arguments(args).build();
    }

    /** サーバ表示ロケールで確定レンダリングした Component（lore / 名前など）。 */
    public static Component render(String key, ComponentLike... args) {
        return GlobalTranslator.render(of(key, args), displayLocale);
    }

    /**
     * サーバ表示ロケールで確定レンダリングし、装飾を除いたプレーン文字列を返す。
     * lore 行を素の {@code String} で組み立てる箇所や、サイドバー/メッセージ引数に名前を渡す箇所向け。
     */
    public static String plain(String key, ComponentLike... args) {
        return PlainTextComponentSerializer.plainText().serialize(render(key, args));
    }

    // ---- 送信ヘルパ ----

    /** チャットへ送る（各クライアント言語）。 */
    public static void send(Audience who, String key, ComponentLike... args) {
        who.sendMessage(of(key, args));
    }

    /** アクションバーへ送る（各クライアント言語）。 */
    public static void actionbar(Audience who, String key, ComponentLike... args) {
        who.sendActionBar(of(key, args));
    }
}
