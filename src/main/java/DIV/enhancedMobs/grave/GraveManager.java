package DIV.enhancedMobs.grave;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.config.MainConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 墓システム（Model B）の中核。{@link Grave} を正本として保持し、所有者の接近で
 * ワールドのダブルチェストを<b>顕現</b>、離れると<b>退避</b>させる。アイテムは常に
 * 「保管（PENDING）」か「チェスト（顕現中）」のどちらか片方のみに存在し、複製しない。
 */
public final class GraveManager {

    private static final BlockFace FACING = BlockFace.NORTH;
    private static final int FORCE_SPAWN_RADIUS = 32;

    private final EnhancedMobs plugin;
    private final MainConfig config;
    private final File file;
    private final NamespacedKey labelKey;

    private final Map<UUID, Grave> graves = new HashMap<>();
    /** 顕現中チェストのブロックキー → 墓（保護判定用）。 */
    private final Map<String, Grave> materializedBlocks = new HashMap<>();
    /** 墓 id → 浮遊ラベルの Entity UUID。 */
    private final Map<UUID, UUID> labels = new HashMap<>();

    public GraveManager(EnhancedMobs plugin, MainConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.file = new File(plugin.getDataFolder(), "graves.yml");
        this.labelKey = new NamespacedKey(plugin, "grave_label");
        load();
        recoverCrashed();
    }

    // ---- 生成 ----

    /**
     * 死亡時に墓を作る。{@code drops} を「復元率」で分割し、回収分（ペナルティ適用済み）は
     * 墓へ収めて drops から取り除く。損失分は drops に残し、死亡地点へ散らばらせる
     * （= 放置率）。墓を作った（drops を一部しまった）なら true。
     */
    public boolean createGraveOnDeath(Player player, List<ItemStack> drops) {
        if (!config.graveEnabled || config.graveDisabledWorlds.contains(player.getWorld().getName())) {
            return false;
        }
        List<ItemStack> items = new ArrayList<>();
        java.util.Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop == null || drop.getType().isAir()) {
                continue;
            }
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= config.graveRecoveryRate) {
                continue; // 損失分: drops に残す（その場に散らばる）
            }
            ItemStack stored = drop.clone();
            applyDurabilityLoss(stored);
            applyXpLoss(stored);
            items.add(stored);
            it.remove(); // 回収分: drops から外す（散らばらせない）
        }
        if (items.isEmpty()) {
            return false; // 回収分が無い（全部その場へ散らばる）
        }
        Location place = findPlacement(player);
        if (place == null) {
            drops.addAll(items); // 念のため（通常 findPlacement は null を返さない）
            return false;
        }
        Grave grave = new Grave(UUID.randomUUID(), player.getUniqueId(), player.getName(),
                place.getWorld().getName(), place.getBlockX(), place.getBlockY(), place.getBlockZ(),
                System.currentTimeMillis());
        grave.items().addAll(items);
        graves.put(grave.id(), grave);
        save(); // 正本を即保存（PENDING）。
        return true;
    }

    /** 墓へ収める耐久ありアイテムが、最大耐久の 0〜durabilityLossMax% をランダムに消耗する（完全破壊はしない）。 */
    private void applyDurabilityLoss(ItemStack item) {
        if (config.graveDurabilityLossMax <= 0) {
            return;
        }
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) {
            return;
        }
        int max = dmg.hasMaxDamage() ? dmg.getMaxDamage() : item.getType().getMaxDurability();
        if (max <= 1) {
            return;
        }
        int loss = (int) Math.round(java.util.concurrent.ThreadLocalRandom.current().nextDouble()
                * (config.graveDurabilityLossMax / 100.0) * max);
        if (loss <= 0) {
            return;
        }
        dmg.setDamage(Math.min(dmg.getDamage() + loss, max - 1)); // 最低1残す
        item.setItemMeta(dmg);
    }

    /** 墓へ収める強化品が、総経験値の xpLoss% を喪失する（レベルを再導出して下げる）。 */
    private void applyXpLoss(ItemStack item) {
        if (config.graveXpLoss <= 0 || !DIV.enhancedMobs.item.ItemEnhancer.isEnhanced(item)) {
            return;
        }
        int total = DIV.enhancedMobs.item.ItemEnhancer.totalInvestedXp(item);
        int kept = (int) Math.round(total * (1.0 - config.graveXpLoss / 100.0));
        DIV.enhancedMobs.item.ItemEnhancer.setLevelFromTotalXp(item, Math.max(0, kept));
    }

    /** /grave コマンドが config で有効か。 */
    public boolean isCommandEnabled() {
        return config.graveCommandEnabled;
    }

    // ---- 場所探索 ----

    private Location findPlacement(Player player) {
        Location death = player.getLocation();
        Location found = locate(death.getWorld(), death.getBlockX(), death.getBlockY(), death.getBlockZ());
        if (found != null) {
            return found;
        }
        Location respawn = player.getRespawnLocation();
        if (respawn == null) {
            respawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        found = locate(respawn.getWorld(), respawn.getBlockX(), respawn.getBlockY(), respawn.getBlockZ());
        if (found != null) {
            return found;
        }
        // 最終手段: スポーン地点付近を強制整地して設置する。
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        return forcePlace(spawn.getWorld(), spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
    }

    /**
     * 最良の設置点を探す。空のある次元（オーバーワールド/エンド）は「死亡地点(x,z)に近い露天の地上」を
     * 最優先（戻りやすい）。見つからない／ネザー等は最寄りの安全な明るい開所へフォールバックする。
     */
    private Location locate(World w, int cx, int cy, int cz) {
        if (hasOpenSky(w)) {
            Location surface = searchSurface(w, cx, cz);
            if (surface != null) {
                return surface;
            }
        }
        return searchOpen(w, cx, cy, cz);
    }

    /** 露天のある次元か（ネザーは天井があるため地表優先しない）。 */
    private static boolean hasOpenSky(World w) {
        return w.getEnvironment() != World.Environment.NETHER;
    }

    /** 死亡地点(x,z)に近い「露天の地上」を半径を広げながら探す。 */
    private Location searchSurface(World w, int cx, int cz) {
        for (int r = 0; r <= config.graveSearchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Integer y = surfaceY(w, cx + dx, cz + dz);
                    if (y != null) {
                        return new Location(w, cx + dx, y, cz + dz);
                    }
                }
            }
        }
        return null;
    }

    /** (bx,bz) と東隣の地表の上に、露天でダブルチェストが置けるなら Y を返す（海/溶岩/被覆はスキップ）。 */
    private Integer surfaceY(World w, int bx, int bz) {
        int y = w.getHighestBlockYAt(bx, bz) + 1; // 最上ブロックの上に置く（陸なら地面、海なら水面の上）
        if (y + 1 >= w.getMaxHeight() || y - 1 <= w.getMinHeight()) {
            return null;
        }
        // fits が下(y-1)=固体非液体・上2マス空を要求 → 海/溶岩の地表は自動でスキップされる。
        return fits(w, bx, y, bz) && fits(w, bx + 1, y, bz)
                && skyExposed(w, bx, bz, y) && skyExposed(w, bx + 1, bz, y) ? y : null;
    }

    /** (bx,bz) で y より上に最上ブロックが無い＝空が見えている（露天）。 */
    private static boolean skyExposed(World w, int bx, int bz, int y) {
        return w.getHighestBlockYAt(bx, bz) < y;
    }

    /** 地下/ネザー含む、最寄りの安全な開所。まず明るい所(光≥7)を優先し、無ければ光不問で最寄りへ。 */
    private Location searchOpen(World w, int cx, int cy, int cz) {
        Location lit = scanOpen(w, cx, cy, cz, true);
        return lit != null ? lit : scanOpen(w, cx, cy, cz, false);
    }

    private Location scanOpen(World w, int cx, int cy, int cz, boolean requireLight) {
        for (int r = 0; r <= config.graveSearchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Integer y = platformY(w, cx + dx, cz + dz, cy);
                    if (y == null) {
                        continue;
                    }
                    if (requireLight && w.getBlockAt(cx + dx, y, cz + dz).getLightLevel() < 7) {
                        continue;
                    }
                    return new Location(w, cx + dx, y, cz + dz);
                }
            }
        }
        return null;
    }

    /** (bx,bz) と東隣 (bx+1,bz) にダブルチェストを置ける Y を返す（無ければ null）。 */
    private Integer platformY(World w, int bx, int bz, int refY) {
        int top = Math.min(refY + 2, w.getMaxHeight() - 2);
        int bottom = w.getMinHeight() + 1;
        for (int y = top; y >= bottom; y--) {
            if (fits(w, bx, y, bz) && fits(w, bx + 1, y, bz)) {
                return y;
            }
        }
        return null;
    }

    /** その柱の Y にチェスト1つが安全に置けるか（足場・空間・ボーダー）。 */
    private boolean fits(World w, int bx, int y, int bz) {
        if (!w.getWorldBorder().isInside(new Location(w, bx + 0.5, y, bz + 0.5))) {
            return false;
        }
        Block below = w.getBlockAt(bx, y - 1, bz);
        Block at = w.getBlockAt(bx, y, bz);
        Block above = w.getBlockAt(bx, y + 1, bz);
        return isSafeGround(below) && isOpen(at) && isOpen(above);
    }

    private static boolean isSafeGround(Block b) {
        Material m = b.getType();
        return m.isSolid() && m != Material.MAGMA_BLOCK && m != Material.LAVA && !b.isLiquid();
    }

    private static boolean isOpen(Block b) {
        return (b.getType().isAir() || b.isReplaceable()) && !b.isLiquid();
    }

    /** その座標に今ダブルチェストを置けるか（顕現直前の最終確認。塞がっていれば再配置する）。 */
    private boolean canPlaceAt(World w, int x, int y, int z) {
        return isSafeGround(w.getBlockAt(x, y - 1, z)) && isSafeGround(w.getBlockAt(x + 1, y - 1, z))
                && isOpen(w.getBlockAt(x, y, z)) && isOpen(w.getBlockAt(x + 1, y, z))
                && isOpen(w.getBlockAt(x, y + 1, z)) && isOpen(w.getBlockAt(x + 1, y + 1, z));
    }

    /** 最終手段の強制整地: スポーン付近で 2 マス分の足場と空間を作って座標を返す。 */
    private Location forcePlace(World w, int cx, int cy, int cz) {
        int y = Math.max(w.getMinHeight() + 2, Math.min(cy, w.getMaxHeight() - 3));
        // スポーン中心から FORCE_SPAWN_RADIUS 内で、整地すれば置ける最寄りを使う（無ければ中心直）。
        int bx = cx;
        int bz = cz;
        // 下を固める・チェスト空間と頭上を空ける。
        for (int dx = 0; dx <= 1; dx++) {
            w.getBlockAt(bx + dx, y - 1, bz).setType(Material.DIRT, false);
            w.getBlockAt(bx + dx, y, bz).setType(Material.AIR, false);
            w.getBlockAt(bx + dx, y + 1, bz).setType(Material.AIR, false);
        }
        return new Location(w, bx, y, bz);
    }

    // ---- 顕現 / 退避 ----

    private void materialize(Grave grave) {
        World w = Bukkit.getWorld(grave.worldName());
        if (w == null || grave.isMaterialized()) {
            return;
        }
        // 設置スポットが塞がっていたら（プレイヤー建築・地形変化・他の墓との衝突）近くの空きへ移す。
        if (!canPlaceAt(w, grave.x(), grave.y(), grave.z())) {
            Location fresh = locate(w, grave.x(), grave.y(), grave.z());
            if (fresh == null) {
                return; // 近くに置ける場所が無い（稀）。PENDING のまま次回再試行。
            }
            grave.relocate(fresh.getBlockX(), fresh.getBlockY(), fresh.getBlockZ());
        }
        Block left = w.getBlockAt(grave.x(), grave.y(), grave.z());
        Block right = w.getBlockAt(grave.x() + 1, grave.y(), grave.z());
        placeDoubleChest(left, right);
        Inventory inv = chestInventory(left);
        if (inv == null) {
            plugin.getLogger().warning("[grave] チェスト設置に失敗 " + grave.worldName()
                    + " (" + grave.x() + "," + grave.y() + "," + grave.z() + ")");
            return;
        }
        for (ItemStack leftover : inv.addItem(grave.items().toArray(new ItemStack[0])).values()) {
            w.dropItemNaturally(left.getLocation().add(0.5, 1, 0.5), leftover);
        }
        grave.items().clear();
        grave.setMaterialized(true);
        materializedBlocks.put(blockKey(grave.worldName(), grave.x(), grave.y(), grave.z()), grave);
        materializedBlocks.put(blockKey(grave.worldName(), grave.x() + 1, grave.y(), grave.z()), grave);
        spawnLabel(grave, w);
        save();
    }

    private void dematerialize(Grave grave) {
        World w = Bukkit.getWorld(grave.worldName());
        if (w == null || !grave.isMaterialized()) {
            return;
        }
        Block left = w.getBlockAt(grave.x(), grave.y(), grave.z());
        Inventory inv = chestInventory(left);
        grave.items().clear();
        if (inv != null) {
            for (ItemStack it : inv.getContents()) {
                if (it != null && !it.getType().isAir()) {
                    grave.items().add(it.clone());
                }
            }
            inv.clear();
        }
        clearBlocks(grave, w);
        grave.setMaterialized(false);
        save();
    }

    private void placeDoubleChest(Block left, Block right) {
        left.setType(Material.CHEST, false);
        right.setType(Material.CHEST, false);
        applyChest(left, Chest.Type.LEFT);
        applyChest(right, Chest.Type.RIGHT);
        // 連結確認: 54枠になっていなければ左右を入れ替える。
        Inventory inv = chestInventory(left);
        if (inv == null || inv.getSize() != 54) {
            applyChest(left, Chest.Type.RIGHT);
            applyChest(right, Chest.Type.LEFT);
        }
    }

    private void applyChest(Block block, Chest.Type type) {
        Chest data = (Chest) block.getBlockData();
        data.setFacing(FACING);
        data.setType(type);
        block.setBlockData(data, false);
    }

    private Inventory chestInventory(Block block) {
        return block.getState() instanceof org.bukkit.block.Chest chest ? chest.getInventory() : null;
    }

    private void clearBlocks(Grave grave, World w) {
        Block left = w.getBlockAt(grave.x(), grave.y(), grave.z());
        Block right = w.getBlockAt(grave.x() + 1, grave.y(), grave.z());
        if (left.getType() == Material.CHEST) {
            ((org.bukkit.block.Chest) left.getState()).getBlockInventory().clear();
            left.setType(Material.AIR, false);
        }
        if (right.getType() == Material.CHEST) {
            ((org.bukkit.block.Chest) right.getState()).getBlockInventory().clear();
            right.setType(Material.AIR, false);
        }
        materializedBlocks.remove(blockKey(grave.worldName(), grave.x(), grave.y(), grave.z()));
        materializedBlocks.remove(blockKey(grave.worldName(), grave.x() + 1, grave.y(), grave.z()));
        removeLabel(grave);
    }

    private void spawnLabel(Grave grave, World w) {
        Location at = new Location(w, grave.x() + 1.0, grave.y() + 1.4, grave.z() + 0.5);
        TextDisplay label = w.spawn(at, TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.text(Component.text(grave.ownerName() + "の墓", NamedTextColor.LIGHT_PURPLE));
            td.getPersistentDataContainer().set(labelKey, PersistentDataType.BYTE, (byte) 1);
        });
        labels.put(grave.id(), label.getUniqueId());
    }

    private void removeLabel(Grave grave) {
        UUID labelId = labels.remove(grave.id());
        if (labelId != null) {
            Entity e = Bukkit.getEntity(labelId);
            if (e != null) {
                e.remove();
            }
        }
    }

    // ---- 周期処理 ----

    /** 所有者の接近で顕現／離脱で退避。失効も処理する（数tick周期で呼ぶ）。 */
    public void tick() {
        double range = config.graveRange;
        double rangeSq = range * range;
        long expiryMillis = (long) (config.graveExpiryDays * 24L * 60L * 60L * 1000L);
        long now = System.currentTimeMillis();
        for (Grave grave : new ArrayList<>(graves.values())) {
            try {
                tickGrave(grave, rangeSq, expiryMillis, now);
            } catch (Exception e) {
                // 1つの墓で例外が出ても周期処理全体を止めない。
                plugin.getLogger().warning("[grave] tick error for " + grave.id() + ": " + e);
            }
        }
    }

    private void tickGrave(Grave grave, double rangeSq, long expiryMillis, long now) {
        if (now - grave.createdMillis() >= expiryMillis) {
            expire(grave);
            return;
        }
        Player owner = Bukkit.getPlayer(grave.ownerId());
        boolean near = owner != null
                && owner.getWorld().getName().equals(grave.worldName())
                && owner.getLocation().distanceSquared(
                        new Location(owner.getWorld(), grave.x() + 0.5, grave.y(), grave.z() + 0.5)) <= rangeSq;
        if (near && !grave.isMaterialized()) {
            materialize(grave);
        } else if (!near && grave.isMaterialized()) {
            dematerialize(grave);
        }
    }

    /** 失効: 中身をその場（チャンクを読み込んで）ドロップし、墓を削除する。 */
    private void expire(Grave grave) {
        World w = Bukkit.getWorld(grave.worldName());
        if (w != null) {
            if (grave.isMaterialized()) {
                dematerialize(grave); // チェスト→items に戻してから
            }
            w.getChunkAt(grave.x() >> 4, grave.z() >> 4); // ドロップのため読み込む
            Location at = new Location(w, grave.x() + 0.5, grave.y() + 0.5, grave.z() + 0.5);
            for (ItemStack it : grave.items()) {
                if (it != null && !it.getType().isAir()) {
                    w.dropItemNaturally(at, it);
                }
            }
        }
        remove(grave);
    }

    // ---- 回収・一覧 ----

    /** 範囲内の自分の墓の中身を手元へ移し、墓を消す。顕現中はチェストから、PENDING は保管から回収。回収数を返す。 */
    public int recoverNearby(Player player) {
        int count = 0;
        double rangeSq = config.graveRange * config.graveRange;
        for (Grave grave : new ArrayList<>(graves.values())) {
            if (!grave.ownerId().equals(player.getUniqueId())
                    || !player.getWorld().getName().equals(grave.worldName())
                    || player.getLocation().distanceSquared(
                            new Location(player.getWorld(), grave.x() + 0.5, grave.y(), grave.z() + 0.5)) > rangeSq) {
                continue;
            }
            if (grave.isMaterialized()) {
                Inventory inv = chestInventory(player.getWorld().getBlockAt(grave.x(), grave.y(), grave.z()));
                if (inv == null) {
                    continue;
                }
                giveAll(player, inv.getContents());
                inv.clear();
            } else {
                giveAll(player, grave.items().toArray(new ItemStack[0]));
            }
            remove(grave);
            count++;
        }
        return count;
    }

    private void giveAll(Player player, ItemStack[] items) {
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) {
                continue;
            }
            for (ItemStack leftover : player.getInventory().addItem(it).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /** プレイヤーから墓までの水平＋垂直距離（同ワールドのみ。別ワールドは -1）。/grave 表示用。 */
    public int distanceTo(Player player, Grave grave) {
        if (!player.getWorld().getName().equals(grave.worldName())) {
            return -1;
        }
        return (int) Math.round(player.getLocation().distance(
                new Location(player.getWorld(), grave.x() + 0.5, grave.y(), grave.z() + 0.5)));
    }

    /** リスポーン直後に、自分の墓の座標と距離をチャットで通知する（1tick遅延で位置確定後に送る）。 */
    public void onRespawn(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> announceGraves(player));
    }

    private void announceGraves(Player player) {
        if (!player.isOnline()) {
            return; // 1tick の間にログアウトした場合
        }
        List<Grave> mine = gravesOf(player);
        if (mine.isEmpty()) {
            return;
        }
        player.sendMessage(Component.text("=== あなたの墓 (" + mine.size() + ") ===", NamedTextColor.LIGHT_PURPLE));
        for (Grave g : mine) {
            int dist = distanceTo(player, g);
            String d = dist < 0 ? "別ワールド" : dist + "m";
            player.sendMessage(Component.text(
                    "墓: " + g.worldName() + " (" + g.x() + ", " + g.y() + ", " + g.z() + ")  距離" + d,
                    NamedTextColor.AQUA));
        }
        player.sendMessage(Component.text("32ブロック以内に行くとチェストが顕現します。/grave で再確認・recover で回収。",
                NamedTextColor.GRAY));
    }

    public List<Grave> gravesOf(Player player) {
        List<Grave> out = new ArrayList<>();
        for (Grave g : graves.values()) {
            if (g.ownerId().equals(player.getUniqueId())) {
                out.add(g);
            }
        }
        return out;
    }

    /** 顕現中チェストのブロックなら所有者 UUID を返す（保護判定）。墓でなければ null。 */
    public Grave graveAt(Block block) {
        return materializedBlocks.get(blockKey(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ()));
    }

    /** チェストが空になったら墓を消す（顕現中チェストを手で空にしたとき）。 */
    public void onChestEmptied(Block block) {
        Grave grave = graveAt(block);
        if (grave == null) {
            return;
        }
        World w = block.getWorld();
        Inventory inv = chestInventory(w.getBlockAt(grave.x(), grave.y(), grave.z()));
        if (inv != null && isEmpty(inv)) {
            remove(grave);
        }
    }

    private static boolean isEmpty(Inventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it != null && !it.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** 墓を完全撤去（ブロック・ラベル・登録）。 */
    private void remove(Grave grave) {
        World w = Bukkit.getWorld(grave.worldName());
        if (grave.isMaterialized() && w != null) {
            clearBlocks(grave, w);
        }
        removeLabel(grave);
        graves.remove(grave.id());
        save();
    }

    // ---- ライフサイクル ----

    /** 無効化時: 顕現中をすべて退避（チェスト→保管・ブロック撤去）。再起動を綺麗にする。 */
    public void onDisable() {
        for (Grave grave : new ArrayList<>(graves.values())) {
            if (grave.isMaterialized()) {
                dematerialize(grave);
            }
        }
        save();
    }

    /** 起動時: ハードクラッシュで顕現中マーカーが残っていたら、ワールドのチェストから中身を回収する。 */
    private void recoverCrashed() {
        for (Grave grave : new ArrayList<>(graves.values())) {
            if (!grave.isMaterialized()) {
                continue;
            }
            World w = Bukkit.getWorld(grave.worldName());
            if (w == null) {
                continue;
            }
            w.getChunkAt(grave.x() >> 4, grave.z() >> 4);
            Inventory inv = chestInventory(w.getBlockAt(grave.x(), grave.y(), grave.z()));
            grave.items().clear();
            if (inv != null) {
                for (ItemStack it : inv.getContents()) {
                    if (it != null && !it.getType().isAir()) {
                        grave.items().add(it.clone());
                    }
                }
            }
            clearBlocks(grave, w);
            grave.setMaterialized(false);
        }
        save();
    }

    // ---- 永続化 ----

    private static String blockKey(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("graves");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                Grave grave = new Grave(UUID.fromString(key), UUID.fromString(s.getString("owner")),
                        s.getString("owner-name", "?"), s.getString("world"),
                        s.getInt("x"), s.getInt("y"), s.getInt("z"), s.getLong("created"));
                grave.setMaterialized(s.getBoolean("materialized"));
                List<?> raw = s.getList("items");
                if (raw != null) {
                    for (Object o : raw) {
                        if (o instanceof ItemStack it) {
                            grave.items().add(it);
                        }
                    }
                }
                graves.put(grave.id(), grave);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("[grave] 不正な墓エントリをスキップ: " + key + " (" + e + ")");
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Grave g : graves.values()) {
            String base = "graves." + g.id();
            yml.set(base + ".owner", g.ownerId().toString());
            yml.set(base + ".owner-name", g.ownerName());
            yml.set(base + ".world", g.worldName());
            yml.set(base + ".x", g.x());
            yml.set(base + ".y", g.y());
            yml.set(base + ".z", g.z());
            yml.set(base + ".created", g.createdMillis());
            yml.set(base + ".materialized", g.isMaterialized());
            yml.set(base + ".items", g.isMaterialized() ? new ArrayList<ItemStack>() : new ArrayList<>(g.items()));
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[grave] graves.yml の保存に失敗: " + e);
        }
    }
}
