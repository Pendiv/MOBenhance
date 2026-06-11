package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 対象のHP比率に応じてダメージを増幅する（満HP=等倍、低HP=低倍率）。 */
public final class WellFedStrikeTrait extends Trait {

    public WellFedStrikeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("well_fed_strike", "WFSTRK", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target instanceof Player) {
            event.setDamage(event.getDamage() * Mobs.healthRatio(target));
        }
    }
}
