package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Periodically teleports near the nearest player. */
public final class SpacetimeLeapTrait extends Trait {

    public SpacetimeLeapTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_leap", "STLEAP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "st_leap_cd")) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 48);
        if (player == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location target = player.getLocation().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
        mob.teleport(target);
        EntityState.setFlag(mob, "st_leap_cd", 100);
    }
}
