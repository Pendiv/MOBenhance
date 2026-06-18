package DIV.enhancedMobs.trait.impl;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;

/** DEMENTOR: 物理ダメージへの耐性と、物理与ダメへの実数加算を持つ（attributelib の物理属性で表現）。 */
public final class DementorTrait extends Trait {

    private final double resistPerRank;
    private final double bonusPerRank;

    public DementorTrait(int cost, int weight, int maxRank, int minLevel,
                         double resistPerRank, double bonusPerRank) {
        super("dementor", "DEMEN", cost, weight, maxRank, minLevel);
        this.resistPerRank = resistPerRank;
        this.bonusPerRank = bonusPerRank;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 物理耐性（近接・矢・三叉槍など物理属性ダメージを割合カット、上限80%）。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.PHYSICAL_RESIST,
                Operation.ADD, Math.min(0.8, resistPerRank * rank));
        // 物理与ダメへの実数加算（旧「ピアス近似」の追加ダメージ）。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.PHYSICAL_DAMAGE_FLAT,
                Operation.ADD, bonusPerRank * rank);
    }
}
