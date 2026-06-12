package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * クリーパー専用。爆発時に爆心地へ残留デバフ雲を放つ。
 * 半径 4+rank、duration (7+3×rank²)秒、終端で半径0へ収縮。
 * デバフは {鈍化/毒/ウィザー/盲目} から毎回1種ランダム（amplifier = rank-1）。
 */
public final class VolatileMixTrait extends Trait {

    private static final PotionEffectType[] DEBUFF_POOL = {
            PotionEffectType.SLOWNESS, PotionEffectType.POISON,
            PotionEffectType.WITHER, PotionEffectType.BLINDNESS,
    };

    public VolatileMixTrait(int cost, int weight, int maxRank, int minLevel) {
        super("volatile_mix", "VOLATILE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onExplosionPrime(LivingEntity mob, int rank, ExplosionPrimeEvent event) {
        // 原典は爆発(Detonate)時に雲を生成。死亡時ではなく自爆でも必ず出る。
        if (event.isCancelled()) {
            return;
        }
        Entity spawned = mob.getWorld().spawnEntity(mob.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        if (!(spawned instanceof AreaEffectCloud cloud)) {
            return;
        }
        float radius = 4.0f + rank;
        int duration = (7 + 3 * rank * rank) * 20;
        cloud.setSource(mob);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration);
        PotionEffectType chosen = DEBUFF_POOL[ThreadLocalRandom.current().nextInt(DEBUFF_POOL.length)];
        cloud.addCustomEffect(new PotionEffect(chosen, duration, rank - 1), true);
    }
}
