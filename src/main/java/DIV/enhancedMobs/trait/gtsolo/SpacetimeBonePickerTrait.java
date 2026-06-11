package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Spacetime predator (approx: lasting strength). */
public final class SpacetimeBonePickerTrait extends Trait {

    public SpacetimeBonePickerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_bone_picker", "STBONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_000_000, rank - 1, true, false, false));
    }
}
