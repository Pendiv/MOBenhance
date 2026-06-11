package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Base for periodic area-of-effect traits: each tick, applies {@link #affect} to every nearby
 * living entity matching the target kind. Covers damage auras, debuff auras, buff-nearby-mobs, etc.
 */
public abstract class AuraTrait extends Trait {

    public enum TargetKind {
        PLAYERS, MOBS, ALL
    }

    private final double range;
    private final TargetKind targetKind;

    protected AuraTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                        double range, TargetKind targetKind) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.range = range;
        this.targetKind = targetKind;
    }

    @Override
    public final void tick(LivingEntity mob, int rank) {
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity target) || target == mob) {
                continue;
            }
            boolean isPlayer = target instanceof Player;
            if (targetKind == TargetKind.PLAYERS && !isPlayer) {
                continue;
            }
            if (targetKind == TargetKind.MOBS && isPlayer) {
                continue;
            }
            affect(mob, rank, target);
        }
    }

    protected abstract void affect(LivingEntity mob, int rank, LivingEntity target);
}
