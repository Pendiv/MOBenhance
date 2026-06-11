package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** 攻撃時20%の確率でダメージを減少または増加させる。倍率はランク+1。 */
public final class GamblerTrait extends Trait {

    public GamblerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("gambler", "GAMBLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= 0.2) {
            return;
        }
        if (random.nextBoolean()) {
            event.setDamage(event.getDamage() / (rank + 1));
        } else {
            event.setDamage(event.getDamage() * (rank + 1));
        }
    }
}
