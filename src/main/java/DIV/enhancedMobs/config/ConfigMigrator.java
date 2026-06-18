package DIV.enhancedMobs.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 意向尊重システム: 起動時に config 群を点検し、バージョン差・破損を検出したら
 * <b>ユーザーの変更を引き継いだまま</b>新フォーマットへ自動移行する。
 *
 * <p>挙動:
 * <ul>
 *   <li>未配置 → jar 同梱のデフォルトをそのまま配置。</li>
 *   <li>{@code config-version} がバンドル側と一致 → 何もしない。</li>
 *   <li>バージョン差 → 同梱の新デフォルト（構造・コメント込み）を土台に、配置済みファイルの
 *       全ての値（葉）を上書きで重ねる。新規キーは既定値で追加され、既存・独自キー（dimensions/
 *       entities にユーザーが追加した独自エントリ等）はすべて保持される（データを失わない）。
 *       旧ファイルは {@code .bak} に退避。</li>
 *   <li>破損（YAML として読めない） → 値を取り出せないので {@code .bak} 退避のうえデフォルトで再生成。</li>
 * </ul>
 *
 * <p>移行はファイル読み込みより前（onEnable 冒頭）に行うため、その起動でそのまま反映される
 * （再起動不要。データパック配備のような別事情での再起動は別途）。</p>
 */
public final class ConfigMigrator {

    /** 各 config の先頭に置くスキーマ世代。デフォルト側を更新したら +1 する。 */
    public static final String VERSION_KEY = "config-version";

    private ConfigMigrator() {
    }

    /**
     * 1 つの config リソースを点検・必要なら移行する。
     *
     * @param plugin       プラグイン
     * @param resourceName jar 同梱リソース名（= データフォルダ内のファイル名）
     */
    public static void migrate(Plugin plugin, String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);

        // 未配置: 通常の初回配置で終わり。
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
            return;
        }

        YamlConfiguration bundled = bundledDefaults(plugin, resourceName);
        if (bundled == null) {
            return; // 同梱リソースが無い（移行対象外）。
        }
        int bundledVersion = bundled.getInt(VERSION_KEY, 0);

        // 配置済みを読み込み（破損検出のため strict load）。
        YamlConfiguration deployed = new YamlConfiguration();
        boolean broken = false;
        try {
            deployed.load(file);
        } catch (InvalidConfigurationException e) {
            broken = true;
        } catch (IOException e) {
            plugin.getLogger().warning("[config] " + resourceName + " の読み込みに失敗（移行をスキップ）: " + e);
            return;
        }

        int deployedVersion = deployed.getInt(VERSION_KEY, 0);
        if (!broken && deployedVersion == bundledVersion) {
            return; // 最新。何もしない。
        }

        backup(plugin, file, resourceName);

        if (broken) {
            // 値を取り出せないため、安全側でデフォルト再生成（旧内容は .bak から手動復元できる）。
            plugin.saveResource(resourceName, true);
            plugin.getLogger().warning("[config] " + resourceName
                    + " が壊れていたためデフォルトで再生成しました（旧ファイルは .bak に退避）。");
            return;
        }

        // 新デフォルト（構造・コメント）を土台に、配置済みの全ての葉の値を重ねる。
        for (String path : deployed.getKeys(true)) {
            if (deployed.isConfigurationSection(path)) {
                continue; // セクション自体はスキップ（葉をコピーすれば親は自動生成される）。
            }
            bundled.set(path, deployed.get(path));
        }
        bundled.set(VERSION_KEY, bundledVersion); // バージョンだけは必ず新世代へ。

        try {
            bundled.save(file);
            plugin.getLogger().info("[config] " + resourceName + " を v" + deployedVersion
                    + " → v" + bundledVersion + " へ移行しました（変更は引き継ぎ済み・旧ファイルは .bak）。");
        } catch (IOException e) {
            plugin.getLogger().warning("[config] " + resourceName + " の保存に失敗: " + e);
        }
    }

    /** jar 同梱のデフォルトを（コメント込みで）読み込む。リソースが無ければ null。 */
    private static YamlConfiguration bundledDefaults(Plugin plugin, String resourceName) {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[config] 同梱 " + resourceName + " の読込に失敗: " + e);
            return null;
        }
    }

    /** 旧ファイルを {@code <name>.bak} へ退避（既存の .bak は上書き）。 */
    private static void backup(Plugin plugin, File file, String resourceName) {
        File bak = new File(file.getParentFile(), resourceName + ".bak");
        try {
            Files.copy(file.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("[config] " + resourceName + " のバックアップに失敗（移行は続行）: " + e);
        }
    }
}
