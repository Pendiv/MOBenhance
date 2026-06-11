package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 周囲8ブロック以内のモブ数に応じて攻撃倍率が上昇する。 */
public final class GroupPsychologyTrait extends Trait {

    public GroupPsychologyTrait(int cost, int weight, int maxRank, int minLevel) {
        super("group_psychology", "GROUP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int count = 0;
        for (Entity entity : mob.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Mob) {
                count++;
            }
        }
        event.setDamage(event.getDamage() * (1.0 + count * 0.06 * rank));
    }
}
