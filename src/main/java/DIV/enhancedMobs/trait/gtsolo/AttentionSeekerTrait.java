package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** Always glowing. */
public final class AttentionSeekerTrait extends Trait {

    public AttentionSeekerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("attention_seeker", "ATTN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
    }
}
