package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 暫定実装: エンドクリスタルギミック（本実装は後フェーズ）の代替として
 * 耐性II（40%カット）+ 再生I + 攻撃力 +(25+25n)% を付与する。
 * 旧実装は rank3 で Resistance V = 恒久無敵となるため上限を II に固定している。
 */
public final class DragonicHeartTrait extends Trait {

    public DragonicHeartTrait(int cost, int weight, int maxRank, int minLevel) {
        super("dragonic_heart", "DRAGON", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_000_000, 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1_000_000, 0, true, false, false));
        // 原典の常時攻撃力強化 +(25+25n)%（クリスタルとは独立）
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_dragonic_atk"),
                0.25 + 0.25 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
