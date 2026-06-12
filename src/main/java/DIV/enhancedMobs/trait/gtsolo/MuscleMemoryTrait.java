package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 受けたステータス上昇効果を永続 modifier として焼き付ける（原典 MuscleMemory）。
 *
 * <p>定期的に自身の有益エフェクトを走査し、attribute modifier を持つものを
 * 永続 modifier に変換する（エフェクトが切れても残る）。同キーは絶対値の大きい方を採用。
 * rank 2 以上では焼き付け量が 105% になる。
 *
 * <p>Bukkit はエフェクト→modifier の対応を公開しないため、バニラの対応表をハードコードする
 * （MOD/データパック由来のカスタムエフェクトは対象外 = 既知の残差）。
 */
public final class MuscleMemoryTrait extends Trait {

    /** バニラの「attribute modifier を持つ有益エフェクト」対応表（量は amplifier+1 に比例）。 */
    private record Mapping(PotionEffectType effect, Attribute attribute, double perLevel,
                           AttributeModifier.Operation op) {
    }

    private static final List<Mapping> MAPPINGS = List.of(
            new Mapping(PotionEffectType.SPEED, Attribute.MOVEMENT_SPEED, 0.20,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1),
            new Mapping(PotionEffectType.STRENGTH, Attribute.ATTACK_DAMAGE, 3.0,
                    AttributeModifier.Operation.ADD_NUMBER),
            new Mapping(PotionEffectType.HASTE, Attribute.ATTACK_SPEED, 0.10,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1),
            new Mapping(PotionEffectType.HEALTH_BOOST, Attribute.MAX_HEALTH, 4.0,
                    AttributeModifier.Operation.ADD_NUMBER),
            new Mapping(PotionEffectType.ABSORPTION, Attribute.MAX_ABSORPTION, 4.0,
                    AttributeModifier.Operation.ADD_NUMBER),
            new Mapping(PotionEffectType.JUMP_BOOST, Attribute.JUMP_STRENGTH, 0.1,
                    AttributeModifier.Operation.ADD_NUMBER),
            new Mapping(PotionEffectType.LUCK, Attribute.LUCK, 1.0,
                    AttributeModifier.Operation.ADD_NUMBER));

    public MuscleMemoryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("muscle_memory", "MUSCLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double factor = rank >= 2 ? 1.05 : 1.0; // 原典: lv2 以上で焼き付け量 105%
        for (PotionEffect effect : mob.getActivePotionEffects()) {
            for (Mapping mapping : MAPPINGS) {
                if (!mapping.effect().equals(effect.getType())) {
                    continue;
                }
                double value = mapping.perLevel() * (effect.getAmplifier() + 1) * factor;
                bake(mob, mapping, value);
            }
        }
    }

    /** 決定的キーで永続 modifier として焼き付け。既存と絶対値比較し強い方のみ採用（原典の上書き規則）。 */
    private static void bake(LivingEntity mob, Mapping mapping, double value) {
        AttributeInstance inst = mob.getAttribute(mapping.attribute());
        if (inst == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(EnhancedMobs.get(), "muscle_"
                + mapping.effect().getKey().getKey() + "_" + mapping.attribute().getKey().getKey());
        AttributeModifier existing = inst.getModifiers().stream()
                .filter(m -> key.equals(m.getKey())).findFirst().orElse(null);
        if (existing != null && Math.abs(existing.getAmount()) >= Math.abs(value)) {
            return;
        }
        if (existing != null) {
            inst.removeModifier(existing);
        }
        inst.addModifier(new AttributeModifier(key, value, mapping.op()));
    }
}
