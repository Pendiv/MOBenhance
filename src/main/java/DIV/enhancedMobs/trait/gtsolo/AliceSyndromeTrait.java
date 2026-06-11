package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;

/** Zombie with flat damage reduction (approx of the variant-shift trait). */
public final class AliceSyndromeTrait extends Trait {

    public AliceSyndromeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("alice_syndrome", "ALICE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Zombie;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        event.setDamage(event.getDamage() * 0.65);
    }
}
