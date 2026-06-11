package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Periodically repositions to a medium distance from the nearest player. */
public final class GateTriumphTrait extends Trait {

    public GateTriumphTrait(int cost, int weight, int maxRank, int minLevel) {
        super("gate_triumph", "GATE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "gate_cd")) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 64);
        if (player == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 16 + random.nextDouble() * 16;
        Location target = player.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        mob.teleport(target);
        EntityState.setFlag(mob, "gate_cd", 2400);
    }
}
