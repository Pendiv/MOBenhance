package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 媒介野 — 自身の足元を継続的に「不明なブロック」（{@link Material#SCULK} 流用。原典と同じく
 * 適正ツールは鍬）へ置き換え続けるブロック罠（原典 MediatorFieldTrait / MediatorFieldBlock）。
 *
 * <p>各罠は元のブロックと 9 種プールからランダムなデバフ 1 種を保持し、プレイヤーが踏むと
 * (5+5N) 秒のデバフ（amplifier 0）を付与して CT 10 秒に入る。同種デバフ保持中は効果時間を加算。
 * <b>破壊されると消えるのではなく元のブロックに復元される</b>（ドロップ・経験値なし）。
 *
 * <p>グリーフィング抑制: 罠はプラグイン内 Map でのみ管理し、チャンクアンロード時・
 * プラグイン無効化時に一括で元のブロックへ復元する（= 罠が永続することはない）。
 * 全体数にも上限があり、超過時は最古の罠から復元・破棄される。
 * 踏み付け/破壊/アンロードのイベントは {@link DIV.enhancedMobs.listener.PlayerListener}
 * から static メソッドへ委譲される。
 */
public final class MediatorFieldTrait extends Trait {

    /** 焼き込むデバフのプール（原典と同じ 9 種）。 */
    private static final PotionEffectType[] DEBUFFS = {
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.HUNGER,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.DARKNESS,
    };

    /** 踏み付け後のクールダウン（CT 10 秒）。 */
    private static final long COOLDOWN_TICKS = 200L;
    /** ワールド全体の罠数上限（超過時は最古から復元・破棄）。 */
    private static final int MAX_TRAPS = 256;

    /** ブロック位置 → 罠データ（挿入順 = 生成順。サーバ起動中のみ保持）。 */
    private static final Map<Location, Trap> TRAPS = new LinkedHashMap<>();

    /** 罠 1 個分のデータ: 元ブロック・デバフ・付与時間・CT。 */
    private static final class Trap {
        final BlockData original;
        final PotionEffectType effect;
        final int durationTicks;
        long ctUntil;

        Trap(BlockData original, PotionEffectType effect, int durationTicks) {
            this.original = original;
            this.effect = effect;
            this.durationTicks = durationTicks;
        }
    }

    public MediatorFieldTrait(int cost, int weight, int maxRank, int minLevel) {
        super("mediator_field", "MEDIATOR", cost, weight, maxRank, minLevel);
    }

    /** 接地中、足元 1 ブロックを罠化する（原典: 10 tick ごと。本実装は特性 tick ごと）。 */
    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!mob.isOnGround()) {
            return;
        }
        Block below = mob.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!isConvertible(below)) {
            return;
        }
        if (TRAPS.size() >= MAX_TRAPS) {
            evictOldest();
        }
        Trap trap = new Trap(below.getBlockData(),
                DEBUFFS[ThreadLocalRandom.current().nextInt(DEBUFFS.length)],
                (5 + 5 * rank) * 20);
        TRAPS.put(below.getLocation(), trap);
        below.setType(Material.SCULK);
    }

    /** 変換対象: フルブロック・BlockEntity なし・破壊可能（岩盤等を除外）・未変換。 */
    private static boolean isConvertible(Block block) {
        Material type = block.getType();
        if (type.isAir() || type == Material.SCULK || TRAPS.containsKey(block.getLocation())) {
            return false;
        }
        if (type.getHardness() < 0 || !type.isOccluding()) {
            return false;
        }
        return !(block.getState() instanceof TileState);
    }

    // ---- グローバルフック（PlayerListener から委譲） ----------------------------

    /** プレイヤーが罠ブロックに乗ったらデバフを付与する（原典 stepOn 相当）。 */
    public static void onPlayerMove(PlayerMoveEvent event) {
        if (TRAPS.isEmpty() || !event.hasChangedBlock()) {
            return;
        }
        Block below = event.getTo().getBlock().getRelative(BlockFace.DOWN);
        Trap trap = TRAPS.get(below.getLocation());
        if (trap == null) {
            return;
        }
        long now = below.getWorld().getFullTime();
        if (now < trap.ctUntil) {
            return; // CT 10 秒。
        }
        Player player = event.getPlayer();
        PotionEffect existing = player.getPotionEffect(trap.effect);
        if (existing == null) {
            player.addPotionEffect(new PotionEffect(trap.effect, trap.durationTicks, 0));
        } else if (existing.getDuration() != PotionEffect.INFINITE_DURATION) {
            // 同種デバフ保持中 → 効果時間を加算（amplifier 維持）。
            player.addPotionEffect(new PotionEffect(trap.effect,
                    existing.getDuration() + trap.durationTicks, existing.getAmplifier()));
        }
        trap.ctUntil = now + COOLDOWN_TICKS;
    }

    /** 罠ブロックは壊すと消えるのではなく元のブロックに復元される（ドロップ・経験値なし）。 */
    public static void onBlockBreak(BlockBreakEvent event) {
        Trap trap = TRAPS.remove(event.getBlock().getLocation());
        if (trap == null) {
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        Block block = event.getBlock();
        EnhancedMobs plugin = EnhancedMobs.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (block.getType().isAir()) {
                block.setBlockData(trap.original);
            }
        });
    }

    /**
     * チャンクアンロード時は域内の罠を一括復元する（永続グリーフィング防止）。
     * 復元の {@code setBlockData} がチャンク処理を再入させて TRAPS を構造変更しうるため、
     * 対象を先に収集してからマップ操作・ブロック復元を行う（イテレート中の変更で
     * ConcurrentModificationException を起こさない）。
     */
    public static void onChunkUnload(ChunkUnloadEvent event) {
        if (TRAPS.isEmpty()) {
            return;
        }
        Chunk chunk = event.getChunk();
        List<Location> targets = new ArrayList<>();
        for (Location loc : TRAPS.keySet()) {
            if (chunk.getWorld().equals(loc.getWorld())
                    && (loc.getBlockX() >> 4) == chunk.getX()
                    && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                targets.add(loc);
            }
        }
        for (Location loc : targets) {
            Trap trap = TRAPS.remove(loc);
            if (trap != null) {
                restore(loc, trap);
            }
        }
    }

    /** プラグイン無効化時に全罠を元のブロックへ復元する（再起動を跨ぐ罠は持たない）。 */
    public static void restoreAll() {
        // restore の setBlockData が再入しても安全なようスナップショットを走査する。
        for (Map.Entry<Location, Trap> entry : new ArrayList<>(TRAPS.entrySet())) {
            if (entry.getKey().isChunkLoaded()) {
                restore(entry.getKey(), entry.getValue());
            }
        }
        TRAPS.clear();
    }

    // ---- 内部処理 --------------------------------------------------------------

    /** 罠数上限の超過時、最古の罠を復元して破棄する。 */
    private static void evictOldest() {
        Iterator<Location> it = TRAPS.keySet().iterator();
        if (!it.hasNext()) {
            return;
        }
        Location loc = it.next();
        // ブロック復元（再入の可能性）より先にマップから外す。
        Trap trap = TRAPS.remove(loc);
        if (trap != null && loc.isChunkLoaded()) {
            restore(loc, trap);
        }
    }

    /** 罠ブロックがまだ残っている場合のみ元のブロックへ戻す（爆発等で別物になっていたら触らない）。 */
    private static void restore(Location loc, Trap trap) {
        Block block = loc.getBlock();
        if (block.getType() == Material.SCULK) {
            block.setBlockData(trap.original);
        }
    }
}
