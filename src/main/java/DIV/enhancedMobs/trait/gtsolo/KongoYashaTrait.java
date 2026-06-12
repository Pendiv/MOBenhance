package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 受ける全ダメージを60%軽減する。プレイヤーからの攻撃には最大HPの0.38%を軽減不可ダメージとして加算。 */
public final class KongoYashaTrait extends Trait {

    public KongoYashaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("kongo_yasha", "KONGO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // /kill・奈落は素通し（原典: BYPASSES_INVULNERABILITY タグ）
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        event.setDamage(event.getDamage() * 0.4);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        // 軽減後にプレイヤー攻撃へ確定チップを加算（onAttacked → onAttackedBy の順で呼ばれる）
        if (attacker instanceof Player) {
            event.setDamage(event.getDamage() + Mobs.maxHealth(mob) * 0.0038);
        }
    }
}
