package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 満HP状態からの一撃即死を無効化して全快する。回数制限なし
 * （一度でも非致死ダメージで削ってから倒すのが攻略法）。
 * <p>ただし蘇生にはCDを設ける: オーラ等の毎tick魔法ダメージで「満HP→即死→全快」を
 * 無限に繰り返すループ（＝音スパムで死に続けて見える不具合）を防ぐため。
 */
public final class IncompleteCombustionTrait extends Trait {

    /** 蘇生CD（tick）。オーラ間隔（20〜40t）より長くしてループを断つ。 */
    private static final int REVIVE_CD = 100;

    public IncompleteCombustionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("incomplete_combustion", "INCOMB", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // 無敵貫通（/kill・奈落）と継続的な環境ダメージ（炎・溶岩・窒息等）は対象外。
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID
                || Mobs.isEnvironmentalDoT(cause) || EntityState.hasFlag(mob, "revive_cd")) {
            return;
        }
        double maxHealth = Mobs.maxHealth(mob);
        boolean wasFull = mob.getHealth() >= maxHealth - 0.001;
        // 原典は防具計算前の amount ≥ HP 判定（BASE ダメージで近似）
        boolean lethal = event.getDamage() >= mob.getHealth();
        if (wasFull && lethal) {
            event.setCancelled(true);
            mob.setHealth(maxHealth);
            EntityState.setFlag(mob, "revive_cd", REVIVE_CD);
            Mobs.playRevivalEffect(mob);
        }
    }
}
