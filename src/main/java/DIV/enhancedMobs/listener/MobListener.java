package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.concurrent.ThreadLocalRandom;

/** スポーン・戦闘・死亡イベントをレベリング・特性・ヘッド表示に繋ぐ。 */
public final class MobListener implements Listener {

    private final EnhancedMobs plugin;

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
    }

    /**
     * 全ダメージを特性にディスパッチ。環境ダメージ（炎・落下・奈落等）も onAttacked に届く。
     * by-entity の場合は飛翔体を射手に解決し、onAttackedBy / onHurtTarget も発火する。
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // STUPEFACTION: マークされたプレイヤーの攻撃は50%で外れる（ミス時は特性発火もなし）。
        if (event instanceof EntityDamageByEntityEvent byEntity
                && event.getEntity() instanceof LivingEntity victim
                && stupefactionMiss(byEntity, victim)) {
            event.setCancelled(true);
            return;
        }
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
        plugin.traitDisplay().cleanup(entity);
    }
}
