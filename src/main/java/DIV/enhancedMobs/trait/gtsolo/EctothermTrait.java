package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SizedFireball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 火炎・溶岩・高温ブロック・凍結・ファイアボールによるダメージをすべて無効化する。 */
public final class EctothermTrait extends Trait {

    public EctothermTrait(int cost, int weight, int maxRank, int minLevel) {
        super("ectotherm", "ECTO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, FREEZE -> event.setCancelled(true);
            default -> {
                // ファイアボール直撃も無効化（原典の IS_FIRE tag 相当。ウィザー頭蓋骨は対象外）
                if (event instanceof EntityDamageByEntityEvent byEntity
                        && byEntity.getDamager() instanceof SizedFireball) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
