package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 常時発光・炎上状態だが、火ダメージは受けない（イベントキャンセル + 火炎耐性）。 */
public final class ShowstopperTrait extends Trait {

    public ShowstopperTrait(int cost, int weight, int maxRank, int minLevel) {
        super("showstopper", "SHOW", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        mob.setGlowing(true);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1_000_000, 0, true, false, false));
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        mob.setFireTicks(Math.max(mob.getFireTicks(), 100));
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 保険: ポーションが剥がされても火ダメージは通さない（原典はイベントキャンセル方式）
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> event.setCancelled(true);
            default -> {
            }
        }
    }
}
