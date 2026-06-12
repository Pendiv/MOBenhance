package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;

/** 有害カテゴリのポーション効果を付与時点で拒否する（バニラ・カスタム問わず全有害効果）。 */
public final class PureHeartTrait extends Trait {

    public PureHeartTrait(int cost, int weight, int maxRank, int minLevel) {
        super("pure_heart", "PURE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onPotionEffect(LivingEntity mob, int rank, EntityPotionEffectEvent event) {
        PotionEffect effect = event.getNewEffect(); // 除去イベントでは null
        if (effect != null && effect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) {
            event.setCancelled(true);
        }
    }
}
