package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Occasionally swaps places with the nearest player. */
public final class WizardryTrait extends Trait {

    public WizardryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("wizardry", "WIZARD", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (ThreadLocalRandom.current().nextDouble() >= 0.02 * rank) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, 16);
        if (player == null || !TraitCooldown.ready(player, "wizardry", 100, 60, 5, 600)) {
            return;
        }
        Location mobLoc = mob.getLocation();
        mob.teleport(player.getLocation());
        player.teleport(mobLoc);
    }
}
