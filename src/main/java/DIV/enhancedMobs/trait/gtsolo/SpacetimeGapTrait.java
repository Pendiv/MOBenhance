package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 時空族: 一定時間（400 - 20rank tick、下限20）攻撃を受けないと全回復する。
 * 全回復に成功すると固定 1200 tick のクールタイムに入る。
 */
public final class SpacetimeGapTrait extends Trait {

    /** 全回復成功後の固定クールダウン。 */
    private static final int HEAL_COOLDOWN = 1200;

    public SpacetimeGapTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_gap", "STGAP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 被弾で無被弾タイマーをリセット（原典: 400 - 20n tick、下限 20）
        EntityState.setFlag(mob, "gap_hit", Math.max(20, 400 - 20 * rank));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "gap_hit") || EntityState.hasFlag(mob, "gap_cd")) {
            return;
        }
        if (mob.getHealth() < Mobs.maxHealth(mob)) {
            // 回復倍率（封印・呪い）を尊重。阻害中は回復できず CD にも入らない（阻害明けに再挑戦）。
            if (Mobs.heal(mob, Mobs.maxHealth(mob)) > 0) {
                EntityState.setFlag(mob, "gap_cd", HEAL_COOLDOWN);
            }
        }
    }
}
