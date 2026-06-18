package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** 全ダメージを軽減する防護マント（attributelib の被ダメ倍率で常時適用。剥がれない）。 */
public final class PhantomMantleTrait extends Trait {

    public PhantomMantleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantom_mantle", "MANTLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 旧: RESISTANCE ポーション(amp=min(3,rank))。被ダメ倍率 = 1 − (min(3,rank)+1)×20% に置換。
        double reduction = (Math.min(3, rank) + 1) * 0.2;
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.DAMAGE_TAKEN,
                Operation.MULTIPLY, 1.0 - reduction);
    }
}
