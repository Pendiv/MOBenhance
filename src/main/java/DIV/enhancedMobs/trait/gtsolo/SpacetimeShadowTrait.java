package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 時空族: 高速移動・高跳躍・段差踏破・遠距離索敵を持ち透明化する奇襲役（ステータスは rank 非依存）。
 * 恒常で移動速度 +150% / 索敵 ×3 / 段差 +3、跳躍力上昇 I を維持。
 * 6m内にプレイヤーがいなければ透明化し攻撃力0（×(-1) MULTIPLY_TOTAL）、接近されると露見して攻撃力が戻る。
 * 露見判定は原典の5tick粒度に合わせ1tickレーンで行う。
 */
public final class SpacetimeShadowTrait extends Trait {

    /** プレイヤーがこの距離内に来ると露見する（原典 REVEAL_DIST）。 */
    private static final double REVEAL_DIST = 6.0;

    private static final NamespacedKey SPEED_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_shadow_spd");
    private static final NamespacedKey RANGE_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_shadow_range");
    private static final NamespacedKey STEP_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_shadow_step");
    private static final NamespacedKey ATK_ZERO_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_shadow_atk0");

    public SpacetimeShadowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_shadow", "STSHADOW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        // 恒常ステータス（原典 postInit・rank 非依存）: 速度 +150% / 索敵 ×3 / 段差 +3。
        Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, SPEED_KEY, 1.5, AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.FOLLOW_RANGE, RANGE_KEY, 2.0, AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.STEP_HEIGHT, STEP_KEY, 3.0, AttributeModifier.Operation.ADD_NUMBER);
        toggle(mob);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        // 跳躍力上昇 I を維持（原典は5tickごとに40tick付与。跳躍attributeの互換のためエフェクトで表現）。
        mob.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                Math.max(40, interval * 2), 0, true, false, false));
        // 露見反応を原典の5tick粒度に近づける1tickレーン（次のtrait tickまで継続し再点火される）。
        int[] life = {interval};
        FastTick.register(mob, "spacetime_shadow", () -> {
            if (!mob.isValid() || --life[0] < 0) {
                return false;
            }
            if (Bukkit.getCurrentTick() % 5 == 0) {
                toggle(mob);
            }
            return true;
        });
    }

    /** 6m内のプレイヤー有無で 透明+攻撃力0 ⇔ 可視+攻撃力復帰 を切り替える。 */
    private static void toggle(LivingEntity mob) {
        if (Mobs.nearestPlayer(mob, REVEAL_DIST) != null) {
            // 露見: 透明解除 + 攻撃力復帰。
            if (mob.isInvisible()) {
                mob.setInvisible(false);
            }
            removeModifier(mob, ATK_ZERO_KEY);
        } else {
            // 潜伏: 透明化 + 攻撃力0（原典 ×(-1.0) MULTIPLY_TOTAL = MULTIPLY_SCALAR_1）。
            if (!mob.isInvisible()) {
                mob.setInvisible(true);
            }
            Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, ATK_ZERO_KEY,
                    -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        }
    }

    private static void removeModifier(LivingEntity mob, NamespacedKey key) {
        AttributeInstance inst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }
}
