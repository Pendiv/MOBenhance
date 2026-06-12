package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 時空族: 近くに他の時空タイプ mob が存在できない。代わりに全ステータスが大幅上昇する。
 * 攻撃力 +(75+25×rank)% / 防具値 +(225+75×rank)% / 移動速度 +(25+6.25×rank)%、
 * rank&gt;1 で防具値 +10×(rank-1) 実数、被ダメージ 20% 軽減（固定）。
 * 周囲16mの他の時空 mob を毎 tick 消滅させる（ドロップなし、原典 discard 相当）。
 */
public final class SpacetimeConquerorTrait extends Trait {

    private static final double SCAN_RADIUS = 16.0;

    public SpacetimeConquerorTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_conqueror", "STCONQ", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_atk"),
                0.75 + 0.25 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ARMOR, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_armor_pct"),
                2.25 + 0.75 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_speed"),
                0.25 + 0.0625 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (rank > 1) {
            Mobs.addModifier(mob, Attribute.ARMOR, new NamespacedKey(EnhancedMobs.get(), "trait_conqueror_armor_flat"),
                    10.0 * (rank - 1), AttributeModifier.Operation.ADD_NUMBER);
        }
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 原典: L2DT REDUCTION +20%（固定）。Bukkit では乗算近似。
        event.setDamage(event.getDamage() * 0.8);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 原典: 20tick ごとの走査（tick-interval 既定 20 でほぼ等価）。
        for (Entity e : mob.getNearbyEntities(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)) {
            if (e instanceof LivingEntity other && !(other instanceof Player)
                    && MobTags.has(other, "spacetime")) {
                other.remove();  // discard 相当（ドロップ・死亡処理なしの即消滅）
            }
        }
    }
}
