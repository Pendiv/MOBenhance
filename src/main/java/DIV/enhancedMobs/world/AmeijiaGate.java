package DIV.enhancedMobs.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.EndGateway;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アメイジアへのゲート。エンドゲートウェイ（END_GATEWAY）= フルブロックで星空/虚空のエンド
 * テクスチャを使う（縦の開口でもちゃんと見える。END_PORTAL は水平面しか描画されないため不可）。
 *
 * <p>登録済みゲートに触れる → アメイジアへ。アメイジア内のゲート → 元の場所（無ければオーバー
 * ワールドのスポーン）へ戻る。バニラのエンドゲートウェイは未登録なので影響しない
 * （{@link DIV.enhancedMobs.listener.GateListener}）。</p>
 *
 * <p>ネイティブの足場生成を避けるため、設置時に ExactTeleport=自分自身 にしておき、実際の
 * テレポートはイベントで横取りして上書きする。</p>
 */
public final class AmeijiaGate {

    /** データパックのディメンションキー（名前空間 = プラグイン名）。 */
    public static final NamespacedKey WORLD_KEY = new NamespacedKey("enhancedmobs", "ameijia");

    /** アメイジア側の到着・帰還足場の基準高さ。 */
    private static final int ARRIVAL_Y = 100;

    /** 登録済みゲート位置（"world;x;y;z"）。バニラのゲートウェイと区別するために必要。 */
    private static final Set<String> gates = ConcurrentHashMap.newKeySet();
    /** プレイヤーごとの入場元（帰還先）。再起動で消える簡易保持。 */
    private static final Map<UUID, Location> origins = new HashMap<>();
    /** テレポート処理中のプレイヤー（移動イベントの多重発火を抑止）。 */
    private static final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();

    private static Plugin plugin;
    private static File file;

    private AmeijiaGate() {
    }

    public static void init(Plugin p) {
        plugin = p;
        file = new File(p.getDataFolder(), "gates.yml");
        load();
    }

    private static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    // ---- ゲートの召喚・判定 ----

    /** 指定ブロックをアメイジア行きゲート（END_GATEWAY）にして登録・保存する。 */
    public static void place(Block block) {
        makeGate(block, true);
    }

    /**
     * 指定ブロックを END_GATEWAY 化してゲート登録する。
     * @param saveNow false なら保存をスキップ（大量設置時のバッチ用。最後に {@link #saveGates()}）。
     */
    public static void makeGate(Block block, boolean saveNow) {
        block.setType(Material.END_GATEWAY, false);
        if (block.getState() instanceof EndGateway gateway) {
            gateway.setExactTeleport(true);
            gateway.setExitLocation(block.getLocation()); // 自己参照=バニラの足場生成を抑止
            gateway.setAge(500L);                         // 紫ビーム（起動済み表示）
            gateway.update(true, false);
        }
        gates.add(key(block));
        if (saveNow) {
            save();
        }
    }

    /** 演出から呼ぶゲート登録（保存はまとめて行うため遅延）。 */
    public static void registerPortal(Block block) {
        makeGate(block, false);
    }

    /** バッチ登録後の保存を明示的に行う。 */
    public static void saveGates() {
        save();
    }

    /** そのブロックが登録済みのアメイジア行きゲートか。 */
    public static boolean isGate(Block block) {
        return gates.contains(key(block));
    }

    /** 足元〜頭付近に登録ゲートがあるか（ゲートウェイは中に入って発動するため範囲広め）。 */
    public static boolean nearGate(Location from) {
        Block feet = from.getBlock();
        return isGate(feet)
                || isGate(feet.getRelative(0, -1, 0))
                || isGate(feet.getRelative(0, 1, 0));
    }

    // ---- テレポート ----

    /** アメイジアへ送る。ワールド未ロードなら false。多重発火は無視して true。 */
    public static boolean toAmeijia(Player player) {
        World world = Bukkit.getWorld(WORLD_KEY);
        if (world == null) {
            return false;
        }
        if (!teleporting.add(player.getUniqueId())) {
            return true; // 既に処理中
        }
        origins.put(player.getUniqueId(), player.getLocation());
        Location arrival = buildArrival(world);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(arrival);
            teleporting.remove(player.getUniqueId());
        });
        return true;
    }

    /** 元の場所（記録が無ければオーバーワールドのスポーン）へ戻す。多重発火は無視。 */
    public static void toOverworld(Player player) {
        if (!teleporting.add(player.getUniqueId())) {
            return; // 既に処理中
        }
        Location origin = origins.remove(player.getUniqueId());
        Location dest = origin != null ? origin : Bukkit.getWorlds().get(0).getSpawnLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(dest);
            teleporting.remove(player.getUniqueId());
        });
    }

    /**
     * アメイジア側の到着足場を用意し、中央の立ち位置を返す。
     * 5x5 の黒曜石床＋頭上クリア、隅に帰還用ゲートを置く（自分で入って戻る）。
     */
    private static Location buildArrival(World world) {
        int bx = world.getSpawnLocation().getBlockX();
        int bz = world.getSpawnLocation().getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(bx + dx, ARRIVAL_Y, bz + dz).setType(Material.OBSIDIAN, false);
                for (int dy = 1; dy <= 3; dy++) {
                    world.getBlockAt(bx + dx, ARRIVAL_Y + dy, bz + dz).setType(Material.AIR, false);
                }
            }
        }
        // 隅に帰還ゲート（中央からは離れているので到着即帰還しない）。
        makeGate(world.getBlockAt(bx + 2, ARRIVAL_Y + 1, bz + 2), true);
        return new Location(world, bx + 0.5, ARRIVAL_Y + 1, bz + 0.5);
    }

    // ---- 永続化 ----

    private static void load() {
        if (file == null || !file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        gates.addAll(yml.getStringList("gates"));
    }

    private static void save() {
        if (file == null) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("gates", new ArrayList<>(gates));
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("ゲート情報の保存に失敗: " + e.getMessage());
        }
    }
}
