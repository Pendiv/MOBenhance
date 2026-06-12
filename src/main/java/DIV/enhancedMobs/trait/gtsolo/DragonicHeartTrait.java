package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ドラコニックハート: 半径8の円周上にエンドクリスタル4基を召喚し、残存中は被ダメージを
 * 90%カットしつつ毎秒 最大体力×0.5% を回復する（クリスタルを全て壊せば解除 = カウンタープレイ）。
 * 攻撃力 +(25+25n)% はクリスタルと独立に常時付与。300t ごとに生存クリスタルを
 * ランダム回転オフセット付きで再配置する。/kill（KILL）と奈落（VOID）は軽減を貫通する。
 *
 * <p>クリスタル親子リンク: 各クリスタルの PDC に親 UUID を記録し、被弾（= エンドクリスタルは
 * 被弾で即起爆する）で親の残数 {@code dragonic_remaining} を減算する。親が死亡・消滅・
 * アンロードしたら紐づくクリスタルを全消去する（原典の owner leave リーシュ相当）。
 * 恒久無敵を生まないこと: 軽減は最大90%で、残数の過大計上（解除不能化）を避けるため
 * 再起動後は実在クリスタルの再走査で残数を実数へ合わせる。
 */
public final class DragonicHeartTrait extends Trait {

    /** クリスタル残存中の被ダメ軽減率（原典: 旧完全無敵 → 90% カットへ弱体化済み）。 */
    private static final double DAMAGE_REDUCTION = 0.9;
    /** クリスタル数（原典: レベル非依存で固定4）。 */
    private static final int CRYSTAL_COUNT = 4;
    /** 配置半径。 */
    private static final double CRYSTAL_RADIUS = 8.0;
    /** 生存クリスタルの再配置周期（原典 300t = 15秒）。 */
    private static final int REPOSITION_INTERVAL_TICKS = 300;
    /** 設置先がブロック内のときの安全地点探索半径（原典と同値）。 */
    private static final int SAFE_SEARCH_RADIUS = 10;
    /** 再起動でトラッカが消えた場合に親ID一致のクリスタルを回収する走査半径。 */
    private static final double RESCAN_RADIUS = 48.0;

    /** 親 UUID → 残存クリスタル UUID 集合（サーバ起動中のキャッシュ。永続は PDC の残数のみ）。 */
    private static final Map<UUID, Set<UUID>> LIVE_CRYSTALS = new ConcurrentHashMap<>();
    /** 親が非ロード中に壊されたクリスタルの減算保留（親の次 tick で適用）。 */
    private static final Map<UUID, Integer> PENDING_DECREMENTS = new ConcurrentHashMap<>();

    public DragonicHeartTrait(int cost, int weight, int maxRank, int minLevel) {
        super("dragonic_heart", "DRAGON", cost, weight, maxRank, minLevel);
        // クリスタルは LivingEntity ではなく MobListener のディスパッチに乗らないため、
        // 破壊検知リスナーを特性側で常設する（onEnable 中の登録）。
        Bukkit.getPluginManager().registerEvents(new CrystalListener(), EnhancedMobs.get());
    }

    private static NamespacedKey parentKey() {
        return new NamespacedKey(EnhancedMobs.get(), "dragonic_parent");
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典の常時攻撃力強化 +(25+25n)%（MULTIPLY_BASE 相当 = ADD_SCALAR。クリスタルと独立）
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_dragonic_atk"),
                0.25 + 0.25 * rank, AttributeModifier.Operation.ADD_SCALAR);
        // クリスタル召喚は1個体につき1回だけ（addTrait 再初期化での二重召喚を防ぐ = 原典 data.spawned）
        if (EntityState.getInt(mob, "dragonic_spawned", 0) != 0) {
            return;
        }
        EntityState.setInt(mob, "dragonic_spawned", 1);
        World world = mob.getWorld();
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < CRYSTAL_COUNT; i++) {
            double angle = (2 * Math.PI * i) / CRYSTAL_COUNT;
            Location spot = findSafeSpot(world,
                    mob.getX() + Math.cos(angle) * CRYSTAL_RADIUS,
                    mob.getY() + 1,
                    mob.getZ() + Math.sin(angle) * CRYSTAL_RADIUS);
            EnderCrystal crystal = world.spawn(spot, EnderCrystal.class, c -> {
                c.setShowingBottom(false);
                c.setGlowing(true);
                c.getPersistentDataContainer().set(parentKey(), PersistentDataType.STRING,
                        mob.getUniqueId().toString());
            });
            ids.add(crystal.getUniqueId());
        }
        EntityState.setInt(mob, "dragonic_remaining", CRYSTAL_COUNT);
        EntityState.setFlag(mob, "dragonic_repos", REPOSITION_INTERVAL_TICKS);
        LIVE_CRYSTALS.put(mob.getUniqueId(), ids);
        registerLeash(mob);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        registerLeash(mob);   // 再起動後の消滅監視の再登録
        applyPendingDecrements(mob);
        int remaining = EntityState.getInt(mob, "dragonic_remaining", 0);
        if (remaining <= 0) {
            return;
        }
        Set<UUID> ids = LIVE_CRYSTALS.get(mob.getUniqueId());
        if (ids == null) {
            // 再起動でトラッカ消失 → 周囲から親ID一致のクリスタルを回収し、残数を実数へ合わせる
            // （見つからないぶんは破壊済み扱い。過大計上 = 解除不能な軽減は作らない）
            ids = rescanCrystals(mob);
            LIVE_CRYSTALS.put(mob.getUniqueId(), ids);
            remaining = ids.size();
            EntityState.setInt(mob, "dragonic_remaining", remaining);
            if (remaining <= 0) {
                return;
            }
        }
        // クリスタル残存中: 毎秒 最大体力×0.5% を回復（tick間隔換算）
        double scale = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval) / 20.0;
        mob.setHealth(Math.min(Mobs.maxHealth(mob),
                mob.getHealth() + Mobs.maxHealth(mob) * 0.005 * scale));
        // 300t ごとに生存クリスタルを再配置
        if (!EntityState.hasFlag(mob, "dragonic_repos")) {
            EntityState.setFlag(mob, "dragonic_repos", REPOSITION_INTERVAL_TICKS);
            reposition(mob, ids);
        }
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // /kill・奈落（原典 BYPASSES_INVULNERABILITY 相当）は軽減を貫通させる
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        if (EntityState.getInt(mob, "dragonic_remaining", 0) > 0) {
            event.setDamage(event.getDamage() * (1.0 - DAMAGE_REDUCTION));
        }
    }

    /** 本体死亡で紐づくクリスタルを全消去する。 */
    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        discardCrystals(mob.getUniqueId());
    }

    /**
     * owner 消滅監視（原典の owner leave → crystals discard のリーシュ相当）。
     * 死亡・デスポーン・チャンクアンロードで owner が無効になったら、紐づくクリスタルを
     * 全消去してトラッカを破棄する。owner が再ロードされた場合は tick の再走査が残数 0 にする
     * （原典でも leash discard は DISCARDED 減算で残数 0 に落ちる）。
     */
    private void registerLeash(LivingEntity mob) {
        if (FastTick.isRegistered(mob, id())) {
            return;
        }
        UUID ownerId = mob.getUniqueId();
        FastTick.register(mob, id(), () -> {
            if (mob.isValid()) {
                return true;
            }
            discardCrystals(ownerId);
            PENDING_DECREMENTS.remove(ownerId);
            return false;
        });
    }

    /** 指定 owner の追跡中クリスタルをロード中のものから全消去する。 */
    private static void discardCrystals(UUID ownerId) {
        Set<UUID> ids = LIVE_CRYSTALS.remove(ownerId);
        if (ids == null) {
            return;
        }
        for (UUID cid : ids) {
            if (Bukkit.getEntity(cid) instanceof EnderCrystal crystal && crystal.isValid()) {
                crystal.getPersistentDataContainer().remove(parentKey());
                crystal.remove();
            }
        }
    }

    /** 非ロード中に積まれた残数減算を適用する。 */
    private static void applyPendingDecrements(LivingEntity mob) {
        Integer pending = PENDING_DECREMENTS.remove(mob.getUniqueId());
        if (pending != null && pending > 0) {
            EntityState.setInt(mob, "dragonic_remaining",
                    Math.max(0, EntityState.getInt(mob, "dragonic_remaining", 0) - pending));
        }
    }

    /** 周囲から親ID一致のクリスタルを回収する（再起動後のトラッカ再構築）。 */
    private static Set<UUID> rescanCrystals(LivingEntity mob) {
        Set<UUID> found = new HashSet<>();
        String ownerId = mob.getUniqueId().toString();
        for (Entity e : mob.getNearbyEntities(RESCAN_RADIUS, RESCAN_RADIUS, RESCAN_RADIUS)) {
            if (e instanceof EnderCrystal crystal && ownerId.equals(
                    crystal.getPersistentDataContainer().get(parentKey(), PersistentDataType.STRING))) {
                found.add(crystal.getUniqueId());
            }
        }
        return found;
    }

    /** 生存クリスタル全体にランダム回転オフセットを与えて円周上へ再配置する。 */
    private static void reposition(LivingEntity mob, Set<UUID> ids) {
        // getEntity == null は「破壊」ではなく「非ロード」の可能性があるため追跡からは外さない
        List<EnderCrystal> alive = new ArrayList<>();
        for (UUID cid : ids) {
            if (Bukkit.getEntity(cid) instanceof EnderCrystal crystal && crystal.isValid()) {
                alive.add(crystal);
            }
        }
        if (alive.isEmpty()) {
            return;
        }
        double rotOffset = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        for (int i = 0; i < alive.size(); i++) {
            double angle = (2 * Math.PI * i) / alive.size() + rotOffset;
            Location spot = findSafeSpot(mob.getWorld(),
                    mob.getX() + Math.cos(angle) * CRYSTAL_RADIUS,
                    mob.getY() + 1,
                    mob.getZ() + Math.sin(angle) * CRYSTAL_RADIUS);
            alive.get(i).teleport(spot);   // クリスタルはパッセンジャー無しのため素の teleport で良い
        }
    }

    /** 設置先がブロック内なら、同心の殻走査（半径ちょうど）で 1×1×2 の空間を探す（原典と同ロジック）。 */
    private static Location findSafeSpot(World world, double x, double y, double z) {
        int sx = (int) Math.floor(x);
        int sy = (int) Math.floor(y);
        int sz = (int) Math.floor(z);
        if (isPlaceable(world, sx, sy, sz)) {
            return center(world, sx, sy, sz);
        }
        for (int radius = 1; radius <= SAFE_SEARCH_RADIUS; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius && Math.abs(dz) != radius) {
                            continue;
                        }
                        if (isPlaceable(world, sx + dx, sy + dy, sz + dz)) {
                            return center(world, sx + dx, sy + dy, sz + dz);
                        }
                    }
                }
            }
        }
        return center(world, sx, sy, sz);
    }

    /** クリスタル設置可能 = 自位置と上1マスが air（1×1×2 の空間）。 */
    private static boolean isPlaceable(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return false;
        }
        return world.getBlockAt(x, y, z).isEmpty() && world.getBlockAt(x, y + 1, z).isEmpty();
    }

    private static Location center(World world, int x, int y, int z) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * クリスタル破壊検知。エンドクリスタルは被弾で即起爆するため、非キャンセルの被弾＝破壊として
     * 親の残数を減算する。多重減算は PDC の親キー除去で防ぐ。
     */
    public static final class CrystalListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onCrystalDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof EnderCrystal crystal)) {
                return;
            }
            String parent = crystal.getPersistentDataContainer().get(parentKey(), PersistentDataType.STRING);
            if (parent == null) {
                return;
            }
            crystal.getPersistentDataContainer().remove(parentKey());
            UUID ownerId;
            try {
                ownerId = UUID.fromString(parent);
            } catch (IllegalArgumentException e) {
                return;
            }
            Set<UUID> ids = LIVE_CRYSTALS.get(ownerId);
            if (ids != null) {
                ids.remove(crystal.getUniqueId());
            }
            if (Bukkit.getEntity(ownerId) instanceof LivingEntity owner && owner.isValid()) {
                EntityState.setInt(owner, "dragonic_remaining",
                        Math.max(0, EntityState.getInt(owner, "dragonic_remaining", 0) - 1));
            } else {
                // 親が非ロード中 → 次の親 tick で減算（取りこぼしは再走査が補正する）
                PENDING_DECREMENTS.merge(ownerId, 1, Integer::sum);
            }
        }
    }
}
