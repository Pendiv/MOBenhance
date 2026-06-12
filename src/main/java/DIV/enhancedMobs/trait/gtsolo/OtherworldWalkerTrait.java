package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * オーバーワールドでは特性自体が消滅。非現世では n = rank + 次元補正
 * （ネザー+1 / エンド+3 / その他+2）を使い、攻撃 +9 かつ +6n%、防御 +10、
 * 索敵範囲 +38 かつ +12n% を得る。
 */
public final class OtherworldWalkerTrait extends Trait {

    public OtherworldWalkerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("otherworld_walker", "OWALK", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        World.Environment env = mob.getWorld().getEnvironment();
        if (env == World.Environment.NORMAL) {
            // 原典準拠: 現世スポーンでは特性が消滅する
            EnhancedMobs.get().traits().stripTrait(mob, "otherworld_walker");
            return;
        }
        int n = rank + switch (env) {
            case NETHER -> 1;
            case THE_END -> 3;
            default -> 2; // カスタム次元
        };
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("ow_atk"), 9,
                AttributeModifier.Operation.ADD_NUMBER);
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("ow_atk_pct"), 0.06 * n,
                AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.ARMOR, key("ow_arm"), 10,
                AttributeModifier.Operation.ADD_NUMBER);
        Mobs.addModifier(mob, Attribute.FOLLOW_RANGE, key("ow_range"), 38,
                AttributeModifier.Operation.ADD_NUMBER);
        Mobs.addModifier(mob, Attribute.FOLLOW_RANGE, key("ow_range_pct"), 0.12 * n,
                AttributeModifier.Operation.ADD_SCALAR);
        // 全回復は generateAndApply の後処理で担保される
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }
}
