package DIV.enhancedMobs.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * 古代都市中心の「基幹ゲート」= バニラ生成の強化深層岩フレームに乗っかる。
 *
 * <p>強化深層岩は通常入手不可で、生成されるのは古代都市中心のこの縦フレーム（厚さ1の垂直面、
 * 開口 約20x6）だけ。よってワールド上の強化深層岩リング＝このゲートと一意に判定でき、構造探索も
 * 自前建築も不要。リカバリーコンパスを枠に使うと開口を検出し、演出後 END_GATEWAY で充填する。</p>
 */
public final class AncientGate {

    private static final Material FRAME = Material.REINFORCED_DEEPSLATE;
    /** フレームと見なす連結強化深層岩の最小数（小さな破片の誤検出防止）。 */
    private static final int MIN_FRAME = 16;
    /** 連結探索の上限。 */
    private static final int MAX_SCAN = 400;

    private static Plugin plugin;

    private AncientGate() {
    }

    public static void init(Plugin p) {
        plugin = p;
    }

    /**
     * 強化深層岩を起点に、それを含む垂直フレームの開口を検出して起動する。
     * @return 起動できたら true（フレーム不正・既に起動済みなら false）。
     */
    public static boolean tryIgnite(Player player, Block clicked) {
        if (clicked.getType() != FRAME) {
            return false;
        }
        Set<Block> ring = collectRing(clicked);
        if (ring.size() < MIN_FRAME) {
            return false;
        }
        // フレームは厚さ1の垂直面（X固定 or Z固定）であること。
        boolean constX = ring.stream().allMatch(b -> b.getX() == clicked.getX());
        boolean constZ = ring.stream().allMatch(b -> b.getZ() == clicked.getZ());
        if (constX == constZ) {
            return false; // 平面でない（両方true=1ブロック、両方false=立体）
        }
        List<Block> opening = openingCells(clicked.getWorld(), ring, constX);
        if (opening.isEmpty() || opening.stream().anyMatch(b -> b.getType() == Material.END_GATEWAY)) {
            return false; // 開口が無い or 既に起動済み
        }
        clearSculkVeins(ring);
        startCinematic(clicked.getWorld(), opening, clicked, constX);
        return true;
    }

    /** 枠の各面に付着したスカルクヴェインを除去する（着火時の見栄え調整）。 */
    private static void clearSculkVeins(Set<Block> ring) {
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (Block b : ring) {
            for (int[] d : dirs) {
                Block n = b.getRelative(d[0], d[1], d[2]);
                if (n.getType() == Material.SCULK_VEIN) {
                    n.setType(Material.CAVE_AIR, false);
                }
            }
        }
    }

    /** 幅軸の座標（平面が X 固定なら Z、Z 固定なら X）。 */
    private static int widthOf(Block b, boolean constX) {
        return constX ? b.getZ() : b.getX();
    }

    /** 連結する強化深層岩（フレーム）を6近傍BFSで収集。 */
    private static Set<Block> collectRing(Block start) {
        Set<Block> seen = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty() && seen.size() < MAX_SCAN) {
            Block b = queue.poll();
            for (int[] d : dirs) {
                Block n = b.getRelative(d[0], d[1], d[2]);
                if (n.getType() == FRAME && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        return seen;
    }

    /**
     * フレームの内部（リング bbox の内側で非フレームの空気/魂の炎セル）を返す。
     * @param constX 平面が X 固定なら true（幅軸=Z）、Z 固定なら false（幅軸=X）。
     */
    private static List<Block> openingCells(World world, Set<Block> ring, boolean constX) {
        int normal = constX ? ring.iterator().next().getX() : ring.iterator().next().getZ();
        int wMin = Integer.MAX_VALUE, wMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (Block b : ring) {
            int w = constX ? b.getZ() : b.getX();
            wMin = Math.min(wMin, w);
            wMax = Math.max(wMax, w);
            yMin = Math.min(yMin, b.getY());
            yMax = Math.max(yMax, b.getY());
        }
        List<Block> opening = new ArrayList<>();
        for (int y = yMin + 1; y <= yMax - 1; y++) {
            for (int w = wMin + 1; w <= wMax - 1; w++) {
                Block b = constX ? world.getBlockAt(normal, y, w) : world.getBlockAt(w, y, normal);
                Material m = b.getType();
                if (m == FRAME) {
                    continue; // 枠の一部（内側に張り出す部分）はスキップ
                }
                // 空気・魂の炎・スカルクヴェイン（枠に付着しがち）を開口として充填対象に。
                if (m == Material.AIR || m == Material.CAVE_AIR
                        || m == Material.SOUL_FIRE || m == Material.SCULK_VEIN) {
                    opening.add(b);
                }
            }
        }
        return opening;
    }

    // ---- 演出 ----

    /** 暗闇＋シュリーカー音 → 2秒待ち → 着火点から左右へ魂の炎 → 両端到達でゲート開通（END_GATEWAY 充填）。 */
    private static void startCinematic(World world, List<Block> opening, Block clicked, boolean constX) {
        int yMin = opening.stream().mapToInt(Block::getY).min().orElseThrow();
        int yMax = opening.stream().mapToInt(Block::getY).max().orElseThrow();
        Location center = opening.stream()
                .map(b -> b.getLocation().add(0.5, 0.5, 0.5))
                .reduce((a, b) -> a.add(b)).orElseThrow()
                .multiply(1.0 / opening.size());

        // 底辺（最下行）を幅座標→ブロックで引けるように。
        TreeMap<Integer, Block> bottom = new TreeMap<>();
        for (Block b : opening) {
            if (b.getY() == yMin) {
                bottom.put(widthOf(b, constX), b);
            }
        }
        int wMin = bottom.firstKey();
        int wMax = bottom.lastKey();
        int ignite = Math.max(wMin, Math.min(wMax, widthOf(clicked, constX)));

        // 開口の各行（下から上へ）= 開通時の充填単位。
        List<List<Block>> rows = new ArrayList<>();
        for (int y = yMin; y <= yMax; y++) {
            final int yy = y;
            rows.add(opening.stream().filter(b -> b.getY() == yy).toList());
        }

        // 暗闇＋スカルクシュリーカー音。
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 24 * 24) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, true, false, true));
            }
        }
        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 3.0f, 1.0f);

        new BukkitRunnable() {
            int wait = 0;   // 開始からの待機tick
            int k = -1;     // 着火点からの広がり半径（-1=未着火）
            int row = 0;    // 充填済み行数
            int phase = 0;  // 0=待機, 1=魂の炎を左右へ, 2=ゲート開通

            @Override
            public void run() {
                switch (phase) {
                    case 0 -> {
                        if (++wait >= 40) { // 2秒待つ
                            phase = 1;
                        }
                    }
                    case 1 -> {
                        k++;
                        boolean leftIn = ignite - k >= wMin;
                        boolean rightIn = ignite + k <= wMax;
                        if (k == 0) {
                            soulFire(bottom.get(ignite), world);
                        } else {
                            if (leftIn) {
                                soulFire(bottom.get(ignite - k), world);
                            }
                            if (rightIn) {
                                soulFire(bottom.get(ignite + k), world);
                            }
                        }
                        // 両側とも端を越えたら開通へ。
                        if (!leftIn && !rightIn) {
                            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.6f);
                            phase = 2;
                        }
                    }
                    case 2 -> {
                        if (row < rows.size()) {
                            for (Block b : rows.get(row)) {
                                AmeijiaGate.registerPortal(b); // END_GATEWAY 化＋登録（保存は後でまとめて）
                                world.spawnParticle(Particle.PORTAL,
                                        b.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.5, 0.3, 0.4);
                            }
                            row++;
                        } else {
                            AmeijiaGate.saveGates();
                            world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 1.0f);
                            cancel();
                        }
                    }
                    default -> cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 3L);
    }

    private static void soulFire(Block b, World world) {
        if (b == null) {
            return; // 底辺に隙間があるセル
        }
        b.setType(Material.SOUL_FIRE, false);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, b.getLocation().add(0.5, 0.3, 0.5),
                10, 0.2, 0.2, 0.2, 0.01);
        world.playSound(b.getLocation(), Sound.BLOCK_SOUL_SAND_HIT, 0.8f, 1.2f);
    }
}
