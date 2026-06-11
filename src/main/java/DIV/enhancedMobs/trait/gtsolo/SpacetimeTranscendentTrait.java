package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/** Annihilates nearby non-spacetime mobs (15% are converted instead). */
public final class SpacetimeTranscendentTrait extends AuraTrait {

    public SpacetimeTranscendentTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_transcendent", "STTRAN", cost, weight, maxRank, minLevel, 8.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (MobTags.has(target, "spacetime")) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            MobTags.add(target, "spacetime");
        } else {
            target.setHealth(0);
        }
    }
}
