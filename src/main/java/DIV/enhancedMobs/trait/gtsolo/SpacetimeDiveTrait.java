package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.DamageElements;
import DIV.attributelib.api.DamageLib;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * 時空族: 潜航状態。魔術系ダメージは等倍で受け、それ以外（物理・矢・爆発・炎など）は大幅軽減する。
 * さらに他MobのAIターゲットから除外される。
 */
public final class SpacetimeDiveTrait extends Trait {

    /** 非魔術ダメージに残す割合（85%カット）。以前は全無効で実質無敵だったため軽減に変更。 */
    private static final double NON_MAGIC_MULT = 0.15;

    public SpacetimeDiveTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_dive", "STDIVE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // attributelib の魔法元素ダメージ（スペルリファクター等）は等倍で通す（DamageCause が CUSTOM でも判定可）。
        if (DamageLib.elementOf(event.getDamageSource()) == DamageElements.MAGIC) {
            return;
        }
        // 原典は forge:is_magic タグのホワイトリスト。魔術相当は等倍、それ以外は大幅軽減（殴り続ければ倒せる）。
        switch (event.getCause()) {
            // 魔術相当: 等倍で通す。
            case MAGIC, WITHER, SONIC_BOOM, DRAGON_BREATH -> {
            }
            // 無効化不能（/kill・奈落・ワールド境界）は潜航でも常に等倍で通す（管理コマンド等を妨げない）。
            case KILL, VOID, WORLD_BORDER -> {
            }
            default -> event.setDamage(event.getDamage() * NON_MAGIC_MULT);
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
