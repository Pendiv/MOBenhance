package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** 常時発光する。 */
public final class AttentionSeekerTrait extends Trait {

    public AttentionSeekerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("attention_seeker", "ATTN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
    }
}
