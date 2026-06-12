package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 初期は移動速度が大幅上昇するが、プレイヤーからの初被弾で解除される。 */
public final class CarriedAwayTrait extends Trait {

    public CarriedAwayTrait(int cost, int weight, int maxRank, int minLevel) {
        super("carried_away", "CARRIED", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典: 移動速度 +(125+25n)%（MULTIPLY_BASE = ADD_SCALAR）。lv1 +150% / lv3 +200%。
        Mobs.addModifier(mob, Attribute.MOVEMENT_SPEED, key(), 1.25 + 0.25 * rank,
                AttributeModifier.Operation.ADD_SCALAR);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return;
        }
        // プレイヤーからの初被弾でモディファイア除去（modifier の有無が状態を兼ねる）。
        AttributeInstance inst = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst == null) {
            return;
        }
        NamespacedKey key = key();
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }

    private static NamespacedKey key() {
        return new NamespacedKey(EnhancedMobs.get(), "trait_carried_away");
    }
}
