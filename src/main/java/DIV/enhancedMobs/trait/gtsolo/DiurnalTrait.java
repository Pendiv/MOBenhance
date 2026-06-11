package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 昼間のみ速度・力・再生を付与し続ける。 */
public final class DiurnalTrait extends Trait {

    public DiurnalTrait(int cost, int weight, int maxRank, int minLevel) {
        super("diurnal", "DIURNAL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob.getWorld().getTime() % 24000 >= 12000) {
            return; // 夜間は何もしない
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, false));
    }
}
