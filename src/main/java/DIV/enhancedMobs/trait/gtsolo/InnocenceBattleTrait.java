package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * プレイヤー以外からの全ダメージ（環境ダメージ含む）と128ブロック超の
 * 遠距離攻撃を無効化し、正面交戦を強制する。
 */
public final class InnocenceBattleTrait extends Trait {

    private static final double MAX_RANGE_SQ = 128.0 * 128.0;

    public InnocenceBattleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("innocence_battle", "INNOCENCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID) {
            return; // 無敵貫通ダメージ（/kill・奈落）は素通し
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            // 射手不明の発射体（ディスペンサーの矢等）はここで拒否。
            // 生きている攻撃者は onAttackedBy 側で Player 判定・距離判定する
            if (resolveLiving(byEntity.getDamager()) == null) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true); // 環境ダメージ（落下・炎上・毒等）は全拒否
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        // 128ブロック超の遠距離スナイプを拒否（ワールド違いも拒否）
        if (attacker.getWorld() != mob.getWorld()
                || attacker.getLocation().distanceSquared(mob.getLocation()) > MAX_RANGE_SQ) {
            event.setCancelled(true);
        }
    }

    private static LivingEntity resolveLiving(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }
}
