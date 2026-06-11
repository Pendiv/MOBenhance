package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

/** 導火線を短縮したクリーパー。最短5ティックまで短縮。 */
public final class HairTriggerTrait extends Trait {

    public HairTriggerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("hair_trigger", "HAIR", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        if (mob instanceof Creeper creeper) {
            creeper.setMaxFuseTicks(Math.max(5, 30 - 4 * rank));
        }
    }
}
