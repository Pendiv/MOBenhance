package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * 観客効果 — 半径24にプレイヤーがいない間、600t（30秒）ごとに強化スタックを獲得する
 * （上限 3+rank）。スタックごとに攻撃力・最大HP・跳躍力 +(10+2×rank)%。
 * プレイヤーが来たらスタックは即0にリセットされる（「見ていない間に化け物になる」）。
 */
public final class AudienceEffectTrait extends Trait {

    private static final double RADIUS = 24.0;
    private static final int GAIN_INTERVAL = 600;

    public AudienceEffectTrait(int cost, int weight, int maxRank, int minLevel) {
        super("audience_effect", "AUDIENCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "aud_last", 0) == 0) {
            EntityState.setInt(mob, "aud_last", Bukkit.getCurrentTick());
        }
        // 変身引き継ぎ等で既存スタックがある場合は属性を貼り直す。
        int stacks = EntityState.getInt(mob, "aud_stacks", 0);
        if (stacks > 0) {
            applyStacks(mob, rank, stacks);
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        int now = Bukkit.getCurrentTick();
        int stacks = EntityState.getInt(mob, "aud_stacks", 0);

        if (Mobs.nearestPlayer(mob, RADIUS) != null) {
            // 観客が来た: スタック即リセット + 蓄積タイマーリセット。
            if (stacks != 0) {
                EntityState.setInt(mob, "aud_stacks", 0);
                applyStacks(mob, rank, 0);
            }
            EntityState.setInt(mob, "aud_last", now);
            return;
        }

        int last = EntityState.getInt(mob, "aud_last", 0);
        if (last == 0 || last > now) {
            // 未初期化・サーバ再起動による tick 巻き戻りはタイマーを引き直す。
            EntityState.setInt(mob, "aud_last", now);
            return;
        }
        if (now - last >= GAIN_INTERVAL && stacks < 3 + rank) {
            EntityState.setInt(mob, "aud_stacks", stacks + 1);
            EntityState.setInt(mob, "aud_last", now);
            applyStacks(mob, rank, stacks + 1);
        }
    }

    /** スタック数に応じた乗算修飾子を貼り直す。最大HPはHP割合を保存して更新する。 */
    private void applyStacks(LivingEntity mob, int rank, int stacks) {
        double amount = stacks * (0.10 + 0.02 * rank);
        EnhancedMobs plugin = EnhancedMobs.get();
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(plugin, "trait_audience_atk"),
                amount, AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.JUMP_STRENGTH, new NamespacedKey(plugin, "trait_audience_jump"),
                amount, AttributeModifier.Operation.ADD_SCALAR);
        double ratio = Mobs.healthRatio(mob);
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(plugin, "trait_audience_hp"),
                amount, AttributeModifier.Operation.ADD_SCALAR);
        mob.setHealth(Math.max(1.0, Math.min(ratio * Mobs.maxHealth(mob), Mobs.maxHealth(mob))));
    }
}
