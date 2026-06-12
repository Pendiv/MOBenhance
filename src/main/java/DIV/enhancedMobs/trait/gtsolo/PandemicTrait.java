package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 接触で他モブに同特性を感染させ、失効ポイントで自然収束する（原典 Pandemic）。
 *
 * <ul>
 *   <li>攻撃倍率: ×(1.3 + 0.1n)（失効後も残存）</li>
 *   <li>感染: 半径2.5内のモブ（プレイヤー非感染）へ親と同 rank で伝播。強い rank 優先で上書きしない</li>
 *   <li>失効: 毎 tick (感染数+1)² ポイント加算（間隔換算）、6000 で失効 → 以後感染停止</li>
 * </ul>
 *
 * <p>感染対象はレベル付与済み（PDC 処理済み）のモブに限定する（未処理個体は tick されないため）。
 */
public final class PandemicTrait extends Trait {

    private static final double CONTACT_RADIUS = 2.5;
    private static final int EXPIRY_THRESHOLD = 6000;
    private static final String INFECTED_KEY = "pand_infected";
    private static final String POINTS_KEY = "pand_pts";
    private static final String EXPIRED_KEY = "pand_expired";

    public PandemicTrait(int cost, int weight, int maxRank, int minLevel) {
        super("pandemic", "PANDEMIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 失効ポイント累積: 原典は毎tick (感染数+1)² → tick間隔換算で加算。
        int infected = EntityState.getInt(mob, INFECTED_KEY, 0);
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        int points = EntityState.addInt(mob, POINTS_KEY, interval * (infected + 1) * (infected + 1));
        if (EntityState.getInt(mob, EXPIRED_KEY, 0) == 1) {
            return; // 失効後は感染停止（攻撃倍率のみ残存）
        }
        if (points >= EXPIRY_THRESHOLD) {
            EntityState.setInt(mob, EXPIRED_KEY, 1);
            return;
        }
        // 感染: バウンディングボックス +2.5 内のモブへ伝播（原典は20t毎 ≒ trait tick間隔）。
        for (Entity entity : mob.getNearbyEntities(CONTACT_RADIUS, CONTACT_RADIUS, CONTACT_RADIUS)) {
            if (entity instanceof Mob other && other != mob) {
                tryInfect(mob, other, rank);
            }
        }
    }

    /** 与ダメ倍率 ×(1.3+0.1n)。与ダメ対象がモブの場合も感染試行（原典準拠: 失効チェックなし）。 */
    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * (1.3 + 0.1 * rank));
        if (target instanceof Mob other) {
            tryInfect(mob, other, rank);
        }
    }

    /** 「より強い rank 優先」で感染（addTrait が同等以上の既存 rank を保持）。成功で親の感染数 +1。 */
    private void tryInfect(LivingEntity mob, Mob target, int rank) {
        if (!MobData.of(target).isProcessed()) {
            return;
        }
        if (EnhancedMobs.get().traits().addTrait(target, id(), rank)) {
            EntityState.addInt(mob, INFECTED_KEY, 1);
            // 原典: 子の感染数カウントは継承時に 0 リセット（子は新たに数え直す）。
            EntityState.setInt(target, INFECTED_KEY, 0);
        }
    }
}
