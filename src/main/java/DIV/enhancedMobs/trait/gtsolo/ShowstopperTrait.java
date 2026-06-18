package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** 常時発光・炎上状態だが、火ダメージは受けない（炎属性軽減100%）。 */
public final class ShowstopperTrait extends Trait {

    public ShowstopperTrait(int cost, int weight, int maxRank, int minLevel) {
        super("showstopper", "SHOW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
        // 火炎耐性 = 炎属性ダメージ軽減 100%（attributelib。溶岩・火・ファイアボール等を一律カット）。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.FIRE_RESIST, Operation.ADD, 1.0);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        mob.setFireTicks(Math.max(mob.getFireTicks(), 100));
    }
}
