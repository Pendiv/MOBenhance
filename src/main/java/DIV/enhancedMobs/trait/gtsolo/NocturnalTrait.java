package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 夜間（ワールド時刻12000以降）に速度・力・再生を付与する。 */
public final class NocturnalTrait extends Trait {

    public NocturnalTrait(int cost, int weight, int maxRank, int minLevel) {
        super("nocturnal", "NOCT", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (mob.getWorld().getTime() % 24000 < 12000) {
            return; // 昼間は何もしない
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, rank - 1, true, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, false));
    }
}
