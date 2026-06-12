package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 時空族: プレイヤーへの攻撃時、相手の残りHP割合が高いほどダメージが増え、防御無視（防具値の足し戻し）も増す。 */
public final class SpacetimeRuptureTrait extends Trait {

    public SpacetimeRuptureTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_rupture", "STRUPT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player)) {
            return;
        }
        double r = Math.clamp(Mobs.healthRatio(player), 0.0, 1.0);
        if (r <= 0) {
            return;
        }
        // (18 + 2rank)% を残HP割合でスケール（満HP・rank1 で +20%）
        double bonus = r * (0.18 + 0.02 * rank);
        AttributeInstance armorInst = player.getAttribute(Attribute.ARMOR);
        double armor = armorInst != null ? armorInst.getValue() : 0;
        // 防御無視は bonus の半分だけ防具値を足し戻す近似（原典 AllOrNothing 方式）
        double restored = event.getDamage() + armor * bonus * 0.5;
        event.setDamage(restored * (1.0 + bonus));
    }
}
