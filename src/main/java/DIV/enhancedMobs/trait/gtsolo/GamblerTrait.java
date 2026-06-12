package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** 受けるダメージが20%の確率で大ブレする。50/50で (ランク+1) 倍または 1/(ランク+1) 倍。 */
public final class GamblerTrait extends Trait {

    public GamblerTrait(int cost, int weight, int maxRank, int minLevel) {
        super("gambler", "GAMBLE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(5) != 0) {
            return; // 80% は素通し
        }
        int factor = rank + 1;
        if (random.nextBoolean()) {
            event.setDamage(event.getDamage() * factor);
        } else {
            event.setDamage(event.getDamage() / factor);
        }
    }
}
