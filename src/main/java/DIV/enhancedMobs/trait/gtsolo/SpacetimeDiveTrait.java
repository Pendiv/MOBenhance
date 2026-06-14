package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/** 時空族: 潜航状態。魔術系ダメージのみ受け付け、それ以外は全て無効化する。さらに他MobのAIターゲットから除外される。 */
public final class SpacetimeDiveTrait extends Trait {

    public SpacetimeDiveTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_dive", "STDIVE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 原典は forge:is_magic タグのホワイトリスト。魔術相当のみ通し、物理・矢・爆発・炎などは全無効。
        switch (event.getCause()) {
            // 魔術相当: 通す。
            case MAGIC, WITHER, SONIC_BOOM, DRAGON_BREATH -> {
            }
            // 無効化不能（/kill・奈落・ワールド境界）は潜航でも常に通す（管理コマンド等を妨げない）。
            case KILL, VOID, WORLD_BORDER -> {
            }
            default -> event.setCancelled(true);
        }
    }

    @Override
    public void onTargeted(LivingEntity mob, int rank, EntityTargetLivingEntityEvent event) {
        // 全MobのAIターゲットから除外（原典 TargetingConditionsMixin 相当）。既存ターゲットも解除。
        event.setCancelled(true);
        if (event.getEntity() instanceof Mob targeter && targeter.getTarget() == mob) {
            targeter.setTarget(null);
        }
    }
}
