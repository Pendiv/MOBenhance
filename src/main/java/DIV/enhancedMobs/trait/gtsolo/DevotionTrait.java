package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/** 体力が半分以下になると即座に死亡し、死亡時に周囲のモブを回復する。 */
public final class DevotionTrait extends Trait {

    public DevotionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("devotion", "DEVOTION", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 自爆トリガー: HP ≤ 50% で即死 → 死亡イベント経由で onDeath が回復を撒く
        if (!mob.isDead() && mob.getHealth() <= Mobs.maxHealth(mob) * 0.5) {
            mob.setHealth(0);
        }
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        double radius = 8 + 2 * rank;
        double heal = Mobs.maxHealth(mob) * (0.25 + 0.125 * rank);
        for (Entity entity : mob.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity other && !(other instanceof Player) && !other.isDead()) {
                other.setHealth(Math.min(Mobs.maxHealth(other), other.getHealth() + heal));
            }
        }
    }
}
