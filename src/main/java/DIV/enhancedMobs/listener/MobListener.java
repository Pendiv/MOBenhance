package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.gtsolo.SorrowElegyTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeBonePickerTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeChainOfCausalityTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeDiffusionTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeHeroTrait;
import DIV.enhancedMobs.trait.gtsolo.SpacetimeInfiniteRecursionTrait;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** スポーン・戦闘・死亡イベントをレベリング・特性・ヘッド表示に繋ぐ。 */
public final class MobListener implements Listener {

    /**
     * ダメージディスパッチの最大再入深さ。反撃（反射・反撃）やオーラが victim.damage() を
     * 呼ぶと本ハンドラが再帰的に発火するため、相互反撃する2体が無限ループして
     * StackOverflow でサーバーが落ちる。この深さを超えたら特性処理を打ち切る
     * （その層のダメージ自体は通すが、さらなる連鎖反撃・オーラは発火させない）。
     */
    private static final int MAX_DISPATCH_DEPTH = 4;

    /** 盾音: 軽減後のベースダメージが元のこの割合以下なら鳴らす。 */
    private static final double SHIELD_FX_RATIO = 0.5;
    /** 盾音のクールタイム（tick、2秒）。被弾対象ごと。 */
    private static final int SHIELD_FX_CD = 40;

    private final EnhancedMobs plugin;
    /** ダメージディスパッチの再入深さ（メインスレッド専用のため非同期化不要）。 */
    private int dispatchDepth;
    /** 盾音判定用: 軽減前のベースダメージ（LOWEST で記録 → MONITOR で比較）。 */
    private final Map<UUID, Double> preReductionDamage = new HashMap<>();

    public MobListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.mainConfig().levelingEnabled) {
            return;
        }
        LivingEntity entity = event.getEntity();
        // Monster は既定で処理対象。非 Monster のボス（エンダードラゴンなど）は
        // per-mob ボーナスが設定されている場合にのみ対象となる。
        if (!(entity instanceof Monster) && !plugin.mobBonus().has(entity.getType())) {
            return;
        }
        if (MobData.of(entity).isProcessed()) {
            return;
        }
        if (!plugin.dimensions().isEnabled(entity.getWorld())) {
            return;
        }
        int level = plugin.difficulty().compute(entity.getLocation());
        level = plugin.mobBonus().apply(entity.getType(), level);
        plugin.initializeMob(entity, level);
        // 時空の敷衍: 新規スポーンへ確率で時空特性を抽選付与（初期化後に判定）。
        SpacetimeDiffusionTrait.onMobSpawn(entity);
    }

    /**
     * 全ダメージを特性にディスパッチ。環境ダメージ（炎・落下・奈落等）も onAttacked に届く。
     * by-entity の場合は飛翔体を射手に解決し、onAttackedBy / onHurtTarget も発火する。
     */
    /** 盾音判定: 軽減が走る前のベースダメージを記録する（全特性・属性軽減より先）。 */
    @EventHandler(priority = EventPriority.LOWEST)
    public void captureBaseDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity victim && MobData.of(victim).isProcessed()) {
            preReductionDamage.put(victim.getUniqueId(), event.getDamage());
        }
    }

    /**
     * 盾音演出: 特性・属性による軽減（イベント系も attributelib の DAMAGE_TAKEN も含む）の結果、
     * ベースダメージが元の50%以下に削られていたら盾で防いだ音を鳴らす（対象ごとに2秒CT）。
     * 完全ブロック（キャンセル＝ジャストブロック等）は別演出があるため対象外。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void shieldSoundFeedback(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Double original = preReductionDamage.remove(victim.getUniqueId());
        if (original == null || original <= 0 || event.isCancelled()) {
            return;
        }
        if (event.getDamage() <= original * SHIELD_FX_RATIO && !EntityState.hasFlag(victim, "shield_fx")) {
            EntityState.setFlag(victim, "shield_fx", SHIELD_FX_CD);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.9f);
        }
    }

    // ignoreCancelled=true: 既にキャンセル済み（ジャストブロック・パリィ等）の被弾では
    // 被ダメ修飾・反撃を一切走らせない（パリィしたのに反撃が飛ぶ/ダメージが復活する事故を防ぐ）。
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // 悲哀の挽歌: 仮死中のエンティティは被弾不可（貫通死は状態を掃除して素通し）。
        if (SorrowElegyTrait.protectPseudoDead(event)) {
            return;
        }
        // STUPEFACTION: マークされたプレイヤーの攻撃は50%で外れる（ミス時は特性発火もなし）。
        if (event instanceof EntityDamageByEntityEvent byEntity
                && event.getEntity() instanceof LivingEntity victim
                && stupefactionMiss(byEntity, victim)) {
            event.setCancelled(true);
            return;
        }
        // 反撃ループの暴走を打ち切る（素のダメージは通し、連鎖反撃・オーラだけ止める）。
        if (dispatchDepth >= MAX_DISPATCH_DEPTH) {
            return;
        }
        dispatchDepth++;
        try {
            if (event.getEntity() instanceof LivingEntity victim && MobData.of(victim).isProcessed()) {
                plugin.traits().onAttacked(victim, event);
                if (event instanceof EntityDamageByEntityEvent byEntity) {
                    LivingEntity attacker = resolveAttacker(byEntity.getDamager());
                    if (attacker != null) {
                        plugin.traits().onAttackedBy(victim, attacker, byEntity);
                    }
                }
            }
            if (event instanceof EntityDamageByEntityEvent byEntity
                    && event.getEntity() instanceof LivingEntity target) {
                LivingEntity attacker = resolveAttacker(byEntity.getDamager());
                if (attacker != null && attacker != target && MobData.of(attacker).isProcessed()) {
                    plugin.traits().onHurtTarget(attacker, target, byEntity);
                }
            }
        } finally {
            dispatchDepth--;
        }
        // 悲哀の挽歌: 致死ダメージの仮死化（被害者自身の復活系特性のキャンセルを先に通す）。
        SorrowElegyTrait.tryPseudoDeath(event);
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * STUPEFACTION 保持者の周囲16mにいたプレイヤー（保持者の tick がフラグ付与）の攻撃判定。
     * 保持者自身への攻撃は特性側の onAttackedBy が処理するため除外（二重判定で実効75%になるのを防ぐ）。
     */
    private boolean stupefactionMiss(EntityDamageByEntityEvent event, LivingEntity victim) {
        if (!(resolveAttacker(event.getDamager()) instanceof Player player)
                || !EntityState.hasFlag(player, "stupefaction_zone")) {
            return false;
        }
        if (MobData.of(victim).isProcessed()
                && plugin.traits().read(victim).keySet().stream().anyMatch(t -> t.id().equals("stupefaction"))) {
            return false;
        }
        return ThreadLocalRandom.current().nextBoolean();
    }

    /** 爆発の起爆を特性に通知（クリーパー系の半径・着火調整用）。 */
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof LivingEntity mob && MobData.of(mob).isProcessed()) {
            plugin.traits().onExplosionPrime(mob, event);
        }
    }

    /** ポーション効果の付与・変更を特性に通知（PURE_HEART 等の付与時拒否用）。 */
    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof LivingEntity mob && MobData.of(mob).isProcessed()) {
            plugin.traits().onPotionEffect(mob, event);
        }
    }

    /** ターゲット設定を被ターゲット側の特性に通知（隠密系の索敵回避用）。 */
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        // 悲哀の挽歌: 仮死中のエンティティは新規ターゲットの対象外。
        if (SorrowElegyTrait.preventTargeting(event)) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target && MobData.of(target).isProcessed()) {
            plugin.traits().onTargeted(target, event);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (MobData.of(entity).isProcessed()) {
            plugin.traits().onDeath(entity, event);
        }
        // 全死亡連動の時空系グローバルフック（保持者は死者ではなく近隣個体）。
        SpacetimeBonePickerTrait.onAnyDeath(entity);
        SpacetimeChainOfCausalityTrait.onAnyDeath(entity);
        // 時空mobの死を近隣の「時空の英雄」へ通知（覚醒カウント）。
        if (MobTags.has(entity, "spacetime")) {
            SpacetimeHeroTrait.onSpacetimeDeath(entity);
        }
        // プレイヤーが「無限再帰」のキャリアなら近隣Mobへ再移譲する。
        if (entity instanceof Player player) {
            SpacetimeInfiniteRecursionTrait.onCarrierDeath(player);
        }
        plugin.traitDisplay().cleanup(entity);
    }
}
