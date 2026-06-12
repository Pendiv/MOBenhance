package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーから受けたダメージ量に応じて回復し、5ヒットごとに必中の反撃を行う。 */
public final class DistantDeathTrait extends Trait {

    public DistantDeathTrait(int cost, int weight, int maxRank, int minLevel) {
        super("distant_death", "DISTDTH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return;
        }
        // 回復は防具適用前のダメージ量基準（原典 getAmount 相当）
        double heal = event.getDamage() * (0.15 + 0.06 * rank);
        mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + heal));
        if (EntityState.addInt(mob, "dd_hits", 1) % 5 == 0) {
            // 自身の攻撃力 × (0.70 + 0.15N) で反撃。無敵時間を消して必中にする（属性なしの種は反撃なし）
            AttributeInstance atk = mob.getAttribute(Attribute.ATTACK_DAMAGE);
            double damage = (atk != null ? atk.getValue() : 0.0) * (0.70 + 0.15 * rank);
            if (damage > 0) {
                attacker.setNoDamageTicks(0);
                attacker.damage(damage, mob);
            }
        }
    }
}
