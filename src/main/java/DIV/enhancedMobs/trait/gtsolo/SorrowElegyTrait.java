package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

/**
 * 悲哀の挽歌 — 周囲（16×ランク m）で死んだ Mob は死亡をキャンセルされ、HP1 の<b>仮死状態</b>として
 * 活動し続ける（原典 SorrowElegy / PseudoDeathHandler）。
 *
 * <p>仮死中は被弾不可（奈落・/kill 等の貫通ダメージは状態を掃除して素通し）・回復不可・
 * 新規 AI ターゲットの対象外。所持者が死亡する（または所持者を見失って 60 秒経過する）と
 * 仮死は一斉に解消される: ランク1=死亡確定、ランク2以上=全快で復活。
 * ランク3では仮死を経た者が仮死後も残る<b>最大体力2倍</b>を獲得する（所持者自身も付与時点で適用）。
 *
 * <p>仮死状態は被害者の PDC（所持者 UUID・ランク）に保存し、孤児検査は FastTick の
 * 個体別ウォッチャー（5 秒周期）で行う。再起動・チャンク再ロードでウォッチャーが消えた場合は
 * 被弾時の保護フックが再登録する。死亡介入・保護・ターゲット除外・回復禁止のグローバルフックは
 * {@link DIV.enhancedMobs.listener.MobListener} / {@link DIV.enhancedMobs.listener.HealListener}
 * から static メソッドへ委譲される。
 */
public final class SorrowElegyTrait extends Trait {

    /** 仮死中の被害者に記録する所持者 UUID（存在 = 仮死中）。 */
    private static final NamespacedKey HOLDER_KEY = new NamespacedKey(EnhancedMobs.get(), "sorrow_pd_holder");
    /** 仮死化時点の所持者ランク（解消時の挙動を決める）。 */
    private static final NamespacedKey RANK_KEY = new NamespacedKey(EnhancedMobs.get(), "sorrow_pd_rank");
    /** 所持者を見失った時刻（ワールド fullTime。再起動を跨いでも単調増加）。 */
    private static final NamespacedKey ORPHAN_KEY = new NamespacedKey(EnhancedMobs.get(), "sorrow_pd_orphan");

    private static final String WATCH_KEY = "sorrow_pd";
    /** 所持者を見失ってから死亡確定までの猶予（= チャンクアンロードと消滅の区別がつかないため）。 */
    private static final long ORPHAN_GRACE = 1200L;
    /** 孤児検査の周期（原典: 5 秒ごと）。 */
    private static final int ORPHAN_CHECK_INTERVAL = 100;

    public SorrowElegyTrait(int cost, int weight, int maxRank, int minLevel) {
        super("sorrow_elegy", "SORROW", cost, weight, maxRank, minLevel);
    }

    /** ランク → 仮死化が起きる半径（16/32/48m）。 */
    private static double radius(int rank) {
        return 16.0 * Math.max(1, rank);
    }

    /** ランク3: 「この効果は自身にも効果がある」= 所持時点で自身も最大体力2倍 + 全快。 */
    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (rank >= 3) {
            applyHpDouble(mob);
            mob.setHealth(Mobs.maxHealth(mob));
        }
    }

    /** 所持者の死亡が確定したら、ワールド内の配下の仮死を一斉解消する。 */
    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        String holderId = mob.getUniqueId().toString();
        for (LivingEntity e : mob.getWorld().getLivingEntities()) {
            if (holderId.equals(e.getPersistentDataContainer().get(HOLDER_KEY, PersistentDataType.STRING))) {
                resolve(e);
            }
        }
    }

    // ---- グローバルフック（MobListener / HealListener から委譲） ----------------

    /**
     * 仮死中のエンティティは被弾不可。貫通ダメージ（奈落・/kill 等）は状態を掃除して素通しし、
     * 本当に死なせる。
     *
     * @return イベントをキャンセルした（= 以降の特性ディスパッチ不要）なら true
     */
    public static boolean protectPseudoDead(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return false;
        }
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        if (!pdc.has(HOLDER_KEY, PersistentDataType.STRING)) {
            return false;
        }
        if (bypasses(event.getCause())) {
            clear(victim);
            return false;
        }
        ensureWatcher(victim); // 再起動・チャンク再ロード後の孤児監視を復帰させる。
        event.setCancelled(true);
        return true;
    }

    /**
     * 致死ダメージを受けた Mob を仮死化する（範囲内に挽歌所持者がいる場合）。
     * 被害者自身の復活系特性（夢に融ける等）のキャンセルを先に通すため、
     * 特性ディスパッチ後・キャンセル済みでない場合にのみ判定する。
     */
    public static void tryPseudoDeath(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Mob victim)) {
            return; // プレイヤー等は対象外。
        }
        if (bypasses(event.getCause()) || victim.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        if (MobData.of(victim).isProcessed() && elegyRank(victim) > 0) {
            return; // 挽歌持ち自身は仮死しない（死 = 解消トリガー）。
        }
        LivingEntity holder = findHolder(victim);
        if (holder == null) {
            return;
        }
        int rank = elegyRank(holder);

        event.setCancelled(true);
        victim.setHealth(1.0);
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        pdc.set(HOLDER_KEY, PersistentDataType.STRING, holder.getUniqueId().toString());
        pdc.set(RANK_KEY, PersistentDataType.INTEGER, rank);
        pdc.remove(ORPHAN_KEY);
        if (rank >= 3) {
            applyHpDouble(victim); // 仮死を経た者は仮死後も残る永続HP2倍。
        }
        ensureWatcher(victim);
    }

    /** 仮死中は回復不可。 */
    public static void preventHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity victim
                && victim.getPersistentDataContainer().has(HOLDER_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    /**
     * 仮死中のエンティティは新規 AI ターゲットの対象外（原典 TargetingConditionsMixin の近似。
     * 既にターゲット中の個体までは解除できないが、攻撃自体は保護で無効化される）。
     *
     * @return イベントをキャンセルしたなら true
     */
    public static boolean preventTargeting(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target != null && target.getPersistentDataContainer().has(HOLDER_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    // ---- 内部処理 --------------------------------------------------------------

    /** 仮死化を貫通するダメージ原因（原典の BYPASSES_INVULNERABILITY 相当）。 */
    private static boolean bypasses(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.SUICIDE;
    }

    /** 範囲内（= 各所持者のランク半径）で最も近い生存所持者。 */
    private static LivingEntity findHolder(LivingEntity victim) {
        double maxR = radius(3);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : victim.getNearbyEntities(maxR, maxR, maxR)) {
            if (!(e instanceof LivingEntity living) || living.isDead() || !MobData.of(living).isProcessed()) {
                continue;
            }
            int rank = elegyRank(living);
            if (rank <= 0) {
                continue;
            }
            double dist = living.getLocation().distance(victim.getLocation());
            if (dist <= radius(rank) && dist < bestDist) {
                best = living;
                bestDist = dist;
            }
        }
        return best;
    }

    private static int elegyRank(LivingEntity mob) {
        for (Map.Entry<Trait, Integer> e : EnhancedMobs.get().traits().read(mob).entrySet()) {
            if (e.getKey().id().equals("sorrow_elegy")) {
                return e.getValue();
            }
        }
        return 0;
    }

    /**
     * 孤児検査ウォッチャー（5 秒周期）。所持者が消滅・死亡したまま {@link #ORPHAN_GRACE} 経過したら
     * 仮死を解消する。対象がアンロードされたら自然消滅し、再ロード後の被弾時に再登録される。
     */
    private static void ensureWatcher(LivingEntity victim) {
        if (FastTick.isRegistered(victim, WATCH_KEY)) {
            return;
        }
        int[] countdown = {ORPHAN_CHECK_INTERVAL};
        FastTick.register(victim, WATCH_KEY, () -> {
            if (!victim.isValid()) {
                return false;
            }
            if (--countdown[0] > 0) {
                return true;
            }
            countdown[0] = ORPHAN_CHECK_INTERVAL;
            PersistentDataContainer pdc = victim.getPersistentDataContainer();
            String raw = pdc.get(HOLDER_KEY, PersistentDataType.STRING);
            if (raw == null) {
                return false; // 解消済み。
            }
            long now = victim.getWorld().getFullTime();
            if (Bukkit.getEntity(UUID.fromString(raw)) instanceof LivingEntity holder
                    && holder.isValid() && !holder.isDead()) {
                pdc.remove(ORPHAN_KEY);
                return true;
            }
            Long since = pdc.get(ORPHAN_KEY, PersistentDataType.LONG);
            if (since == null) {
                pdc.set(ORPHAN_KEY, PersistentDataType.LONG, now);
                return true;
            }
            if (now - since >= ORPHAN_GRACE) {
                resolve(victim);
                return false;
            }
            return true;
        });
    }

    /** 仮死の解消: ランク1=死亡確定、ランク2以上=全快で復活。 */
    private static void resolve(LivingEntity victim) {
        int rank = victim.getPersistentDataContainer().getOrDefault(RANK_KEY, PersistentDataType.INTEGER, 1);
        clear(victim);
        FastTick.unregister(victim, WATCH_KEY);
        if (rank >= 2) {
            victim.setHealth(Mobs.maxHealth(victim));
        } else {
            victim.setHealth(0.0); // setHealth(0) はダメージイベントを介さない確殺（トーテム不発）。
        }
    }

    private static void clear(LivingEntity victim) {
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        pdc.remove(HOLDER_KEY);
        pdc.remove(RANK_KEY);
        pdc.remove(ORPHAN_KEY);
    }

    /** 永続の最大体力2倍（原典: MULTIPLY_TOTAL +1.0。冪等）。 */
    private static void applyHpDouble(LivingEntity mob) {
        Mobs.addModifier(mob, Attribute.MAX_HEALTH,
                new NamespacedKey(EnhancedMobs.get(), "trait_sorrow_hp2"),
                1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
