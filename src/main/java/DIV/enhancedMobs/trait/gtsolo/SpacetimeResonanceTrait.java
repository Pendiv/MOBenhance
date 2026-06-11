package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Strengthens nearby spacetime mobs. */
public final class SpacetimeResonanceTrait extends AuraTrait {

    public SpacetimeResonanceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_resonance", "STRESO", cost, weight, maxRank, minLevel, 12.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (MobTags.has(target, "spacetime")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        }
    }
}
