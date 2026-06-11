package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 攻撃プレイヤーの武器攻撃力（基本値）だけ受けるダメージを差し引く。 */
public final class ParadiseLostTrait extends Trait {

    public ParadiseLostTrait(int cost, int weight, int maxRank, int minLevel) {
        super("paradise_lost", "PARADISE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player player) {
            AttributeInstance attack = player.getAttribute(Attribute.ATTACK_DAMAGE);
            double base = attack != null ? attack.getValue() : 1.0;
            event.setDamage(Math.max(0, event.getDamage() - base));
        }
    }
}
