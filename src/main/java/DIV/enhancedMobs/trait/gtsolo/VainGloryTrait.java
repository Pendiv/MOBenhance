package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 特殊体力（衝撃吸収）に移行する。最大HPの200%の吸収値を持ち、
 * 全ての被ダメージはバニラの absorption-first 規則で吸収から減る。
 * 吸収が尽きるか、吸収を抜けて本体HPが削れたら即死亡する（回復は実質無意味）。
 */
public final class VainGloryTrait extends Trait {

    public VainGloryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("vain_glory", "VAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        apply(mob);
        // TANK 等が最大HPを増やす順序は不定のため、確定後の最大HPで1tick後に再設定する。
        Bukkit.getScheduler().runTask(EnhancedMobs.get(), () -> {
            if (mob.isValid()) {
                apply(mob);
            }
        });
    }

    /** 原典: setAbsorptionAmount(maxHealth × 2.0)。rank 非依存で一律200%。 */
    private static void apply(LivingEntity mob) {
        mob.setAbsorptionAmount(Mobs.maxHealth(mob) * 2.0);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        check(mob);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 吸収消費・本体HP侵食はイベント適用後に確定するため、1tick遅延して判定する
        // （tick粒度1秒の「吸収切れ→死亡」の遅れをほぼ解消）。
        Bukkit.getScheduler().runTask(EnhancedMobs.get(), () -> {
            if (mob.isValid()) {
                check(mob);
            }
        });
    }

    /** 原典 tick と同一ロジック: 吸収 ≤0 → 死亡、本体HPが最大値を割っていても死亡。 */
    private static void check(LivingEntity mob) {
        if (mob.isDead()) {
            return;
        }
        if (mob.getAbsorptionAmount() <= 0.0) {
            mob.setHealth(0);
            return;
        }
        if (mob.getHealth() < Mobs.maxHealth(mob)) {
            mob.setHealth(0);
        }
    }
}
