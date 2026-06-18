package DIV.enhancedMobs.trait.impl;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 自身に火耐性を付与し、ヒット時に対象を着火する。 */
public final class FieryTrait extends Trait {

    private final int fireSeconds;

    public FieryTrait(int cost, int weight, int maxRank, int minLevel, int fireSeconds) {
        super("fiery", "FIERY", cost, weight, maxRank, minLevel);
        this.fireSeconds = fireSeconds;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 炎無効 = 炎属性ダメージ軽減 100%（attributelib。溶岩・火・ファイアボール等のバニラ炎も全カット）。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.FIRE_RESIST, Operation.ADD, 1.0);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }
        target.setFireTicks(Math.max(target.getFireTicks(), fireSeconds * 20));
    }
}
