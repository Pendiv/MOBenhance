package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 火炎・溶岩・高温ブロック・凍結・ファイアボールによるダメージをすべて無効化する。 */
public final class EctothermTrait extends Trait {

    public EctothermTrait(int cost, int weight, int maxRank, int minLevel) {
        super("ectotherm", "ECTO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 熱（炎属性）無効 = 炎属性ダメージ軽減 100%（attributelib）。溶岩・火・ファイアボールも一律カット。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.FIRE_RESIST, Operation.ADD, 1.0);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 凍結は元素を持たないため従来どおりイベントで無効化（炎・溶岩・ファイアボールは FIRE_RESIST が処理）。
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
            event.setCancelled(true);
        }
    }
}
