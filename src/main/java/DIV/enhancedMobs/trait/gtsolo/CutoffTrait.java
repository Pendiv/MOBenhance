package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 最大HP1%未満の微小ダメージを無効化する。 */
public final class CutoffTrait extends Trait {

    public CutoffTrait(int cost, int weight, int maxRank, int minLevel) {
        super("cutoff", "CUTOFF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 原典は防具適用前の amount で判定するため getDamage()（raw）を使う
        if (event.getDamage() < Mobs.maxHealth(mob) * 0.01) {
            event.setCancelled(true);
        }
    }
}
