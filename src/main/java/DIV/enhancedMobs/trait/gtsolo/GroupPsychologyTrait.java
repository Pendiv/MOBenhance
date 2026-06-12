package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 周囲16ブロック以内のモブ数に応じて攻撃倍率が上昇する（1体につき +6%×ランク）。 */
public final class GroupPsychologyTrait extends Trait {

    public GroupPsychologyTrait(int cost, int weight, int maxRank, int minLevel) {
        super("group_psychology", "GROUP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        int count = 0;
        for (Entity entity : mob.getNearbyEntities(16, 16, 16)) {
            if (entity instanceof Mob) {
                count++;
            }
        }
        event.setDamage(event.getDamage() * (1.0 + count * 0.06 * rank));
    }
}
