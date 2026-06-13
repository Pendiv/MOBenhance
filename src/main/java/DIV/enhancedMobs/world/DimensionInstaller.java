package DIV.enhancedMobs.world;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * jar に同梱したカスタムディメンションのデータパック（dimension / dimension_type / worldgen）を
 * メインワールドの datapacks フォルダへ配備する。
 *
 * <p>データパックはワールドロード時に読み込まれるため、配備はワールドロード前（onLoad）に行う。
 * 既存ファイルと内容が一致すれば何もしない。初回配備時はその起動では未読込なので、
 * 反映には再起動が1回必要（attributelib のダメージタイプと同じ事情）。</p>
 */
public final class DimensionInstaller {

    private static final String PACK_DIR_NAME = "emob_dimensions";

    /**
     * このデータパックが対象とする Minecraft バージョン接頭辞。worldgen の JSON スキーマは
     * バージョン依存のため、想定外バージョンへ配備すると最悪サーバーが起動できなくなる。
     * {@link org.bukkit.Bukkit#getMinecraftVersion()} がこれで始まらない場合は配備をスキップする。
     */
    private static final String SUPPORTED_VERSION_PREFIX = "26.1";

    private static final List<String> FILES = List.of(
            "pack.mcmeta",
            "data/enhancedmobs/dimension/ameijia.json",
            "data/enhancedmobs/dimension_type/ameijia.json",
            "data/enhancedmobs/worldgen/biome/ameijia_wastes.json",
            "data/enhancedmobs/worldgen/biome/ameijia_deep_dark.json",
            "data/enhancedmobs/worldgen/noise_settings/ameijia.json",
            "data/enhancedmobs/worldgen/structure/ancient_city.json",
            "data/enhancedmobs/worldgen/structure_set/ancient_cities.json",
            "data/enhancedmobs/worldgen/structure/end_city.json",
            "data/enhancedmobs/worldgen/structure_set/end_cities.json",
            "data/enhancedmobs/worldgen/configured_carver/cave_dense.json",
            "data/enhancedmobs/worldgen/configured_carver/cave_extra_dense.json",
            "data/enhancedmobs/worldgen/configured_carver/canyon_dense.json",
            "data/enhancedmobs/worldgen/configured_feature/ore_iron_block.json",
            "data/enhancedmobs/worldgen/configured_feature/ore_gold_block.json",
            "data/enhancedmobs/worldgen/configured_feature/ore_diamond_block.json",
            "data/enhancedmobs/worldgen/configured_feature/ore_emerald_block.json",
            "data/enhancedmobs/worldgen/configured_feature/ore_ancient_debris.json",
            "data/enhancedmobs/worldgen/placed_feature/ore_iron_block.json",
            "data/enhancedmobs/worldgen/placed_feature/ore_gold_block.json",
            "data/enhancedmobs/worldgen/placed_feature/ore_diamond_block.json",
            "data/enhancedmobs/worldgen/placed_feature/ore_emerald_block.json",
            "data/enhancedmobs/worldgen/placed_feature/ore_ancient_debris.json"
    );

    private DimensionInstaller() {
    }

    /** @return 書き込んだ（新規+更新）ファイル数。0 なら配備済みで変更なし、または非対応バージョンでスキップ。 */
    public static int install(Plugin plugin) {
        Path packDir = mainWorldFolder().resolve("datapacks").resolve(PACK_DIR_NAME);

        // バージョンガード: worldgen スキーマは MC バージョン依存。非対応バージョンに配備すると
        // 「Failed to load datapacks」でサーバーが起動不能になるため、その前に配備を中止する。
        String mcVersion = serverMinecraftVersion();
        if (mcVersion != null && !mcVersion.startsWith(SUPPORTED_VERSION_PREFIX)) {
            plugin.getLogger().warning("カスタムディメンションは MC " + SUPPORTED_VERSION_PREFIX
                    + ".x 用です。現在は " + mcVersion + " のため配備をスキップします"
                    + "（worldgen 形式が非互換でサーバーが起動できなくなる恐れ）。");
            quarantineIfPresent(plugin, packDir, mcVersion);
            return 0;
        }

        int written = 0;
        for (String file : FILES) {
            try (InputStream in = plugin.getResource("dimensions/" + file)) {
                if (in == null) {
                    throw new IllegalStateException("jar 内にディメンションリソースがありません: " + file);
                }
                byte[] content = in.readAllBytes();
                Path target = packDir.resolve(file);
                if (Files.exists(target) && Arrays.equals(Files.readAllBytes(target), content)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.write(target, content);
                written++;
            } catch (IOException e) {
                throw new IllegalStateException("ディメンションデータパックの配備に失敗しました: " + file, e);
            }
        }
        if (written > 0) {
            plugin.getLogger().info("ディメンションデータパックを配備しました (" + written + " ファイル): " + packDir);
        }
        return written;
    }

    /** サーバーの MC バージョン文字列（例 "26.1.2"）。取得できなければ null（ガードはフェイルオープン）。 */
    private static String serverMinecraftVersion() {
        try {
            return Bukkit.getMinecraftVersion();
        } catch (Throwable t) {
            return null; // 想定外のフォーク等。判定不能なら従来通り配備する。
        }
    }

    /**
     * 非対応バージョン時、過去に配備済みのパックが datapacks 配下に残っていると
     * 起動時パースで落ちる。datapacks の外へ退避してサーバー起動を守る（データは保全）。
     */
    private static void quarantineIfPresent(Plugin plugin, Path packDir, String mcVersion) {
        if (!Files.isDirectory(packDir)) {
            return;
        }
        Path quarantine = packDir.getParent().getParent()
                .resolve(PACK_DIR_NAME + "_incompatible_" + mcVersion.replaceAll("[^0-9A-Za-z.]", "_"));
        try {
            Files.move(packDir, quarantine);
            plugin.getLogger().warning("配備済みデータパックを datapacks の外へ退避しました: " + quarantine
                    + "（対応バージョンへ更新後、形式を移行して再配備してください）。");
        } catch (IOException e) {
            plugin.getLogger().severe("非互換データパックの退避に失敗しました。サーバーが起動できない場合は "
                    + packDir + " を手動で削除してください: " + e.getMessage());
        }
    }

    /**
     * メインワールドのフォルダ。onLoad 時点ではワールドが未ロードのため
     * server.properties の level-name から解決する。
     */
    private static Path mainWorldFolder() {
        Path container = Bukkit.getWorldContainer().toPath();
        Path properties = container.resolve("server.properties");
        String levelName = "world";
        if (Files.exists(properties)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(properties)) {
                props.load(in);
                levelName = props.getProperty("level-name", "world");
            } catch (IOException e) {
                // 読めなければ既定値 "world" で続行
            }
        }
        return container.resolve(levelName);
    }
}
