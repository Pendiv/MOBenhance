package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/** オーバーワールド以外（ネザー/エンド/カスタム次元）でのみ攻撃・防御・索敵を強化する。 */
public final class OtherworldWalkerTrait extends Trait {

    public OtherworldWalkerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("otherworld_walker", "OWALK", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (mob.getWorld().getEnvironment() == World.Environment.NORMAL) {
            return;
        }
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, key("ow_atk"), 9 + rank,
                AttributeModifier.Operation.ADD_NUMBER);
        Mobs.addModifier(mob, Attribute.ARMOR, key("ow_arm"), 10,
                AttributeModifier.Operation.ADD_NUMBER);
        Mobs.addModifier(mob, Attribute.FOLLOW_RANGE, key("ow_range"), 38,
                AttributeModifier.Operation.ADD_NUMBER);
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), name);
    }
}
