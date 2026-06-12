package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 満HP状態からの一撃即死を無効化して全快する。回数制限なし
 * （一度でも非致死ダメージで削ってから倒すのが攻略法）。
 */
public final class IncompleteCombustionTrait extends Trait {

    public IncompleteCombustionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("incomplete_combustion", "INCOMB", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID) {
            return; // 無敵貫通ダメージ（/kill・奈落）は素通し
        }
        double maxHealth = Mobs.maxHealth(mob);
        boolean wasFull = mob.getHealth() >= maxHealth - 0.001;
        // 原典は防具計算前の amount ≥ HP 判定（BASE ダメージで近似）
        boolean lethal = event.getDamage() >= mob.getHealth();
        if (wasFull && lethal) {
            event.setCancelled(true);
            mob.setHealth(maxHealth);
        }
    }
}
