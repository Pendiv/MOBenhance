package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

/** Cleanses debuffs from nearby spacetime mobs. */
public final class SpacetimeTidalForceTrait extends AuraTrait {

    private static final PotionEffectType[] DEBUFFS = {
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS,
            PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE
    };

    public SpacetimeTidalForceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_tidal_force", "STTIDAL", cost, weight, maxRank, minLevel, 12.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (MobTags.has(target, "spacetime")) {
            for (PotionEffectType type : DEBUFFS) {
                target.removePotionEffect(type);
            }
        }
    }
}
