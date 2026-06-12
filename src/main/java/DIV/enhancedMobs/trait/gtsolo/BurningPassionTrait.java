package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 初めて炎系ダメージを受けると覚醒（永続フラグ）。覚醒後は炎ダメージを完全無効化し、
 * 永続燃焼ビジュアルを維持しつつ、攻撃力 +(10+5n)% と毎秒 最大HP×0.35n% の回復を得る。
 * 点火されるまでは無能力（原典準拠の段階構造）。
 */
public final class BurningPassionTrait extends Trait {

    public BurningPassionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("burning_passion", "PASSION", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> {
                if (EntityState.getInt(mob, "bp_ignited", 0) == 0) {
                    // 初被炎で覚醒（このダメージ自体は受ける）
                    EntityState.setInt(mob, "bp_ignited", 1);
                    applyAtkBuff(mob, rank);
                } else {
                    // 覚醒後は炎完全無効（fireTicks は残るので燃焼ビジュアルは保たれる）
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "bp_ignited", 0) == 0) {
            return;
        }
        // 永続燃焼ビジュアルの維持（残り 5 秒を切ったら 10 秒へ戻す）
        if (mob.getFireTicks() < 100) {
            mob.setFireTicks(200);
        }
        // 毎秒 最大HP×0.35n% 回復（原典は setHealth 直書き。tick 間隔換算）
        if (!mob.isDead()) {
            int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
            double max = Mobs.maxHealth(mob);
            mob.setHealth(Math.min(max, mob.getHealth() + max * 0.0035 * rank * interval / 20.0));
        }
        // ATK バフの付け直し保証（チャンク再ロード保険。冪等）
        applyAtkBuff(mob, rank);
    }

    /** 攻撃力 +(10+5n)%（原典 MULTIPLY_BASE = Bukkit の ADD_SCALAR）。 */
    private static void applyAtkBuff(LivingEntity mob, int rank) {
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_burning_passion_atk"),
                0.10 + 0.05 * rank, AttributeModifier.Operation.ADD_SCALAR);
    }
}
