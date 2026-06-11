package DIV.enhancedMobs.trait;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A mob trait. Singletons (one instance, applied to many mobs with a rank), mirroring
 * L2Hostility's {@code MobTrait}. Metadata fields drive the weighted draw in
 * {@link TraitGenerator}; the hooks implement the actual behaviour.
 */
public abstract class Trait {

    private final String id;
    private final String shortName;
    private final int cost;
    private final int weight;
    private final int maxRank;
    private final int minLevel;

    protected Trait(String id, String shortName, int cost, int weight, int maxRank, int minLevel) {
        this.id = id;
        this.shortName = shortName;
        this.cost = cost;
        this.weight = weight;
        this.maxRank = maxRank;
        this.minLevel = minLevel;
    }

    public final String id() {
        return id;
    }

    /** Abbreviated label shown above the mob's head, e.g. "TANK". */
    public final String shortName() {
        return shortName;
    }

    public final int cost() {
        return cost;
    }

    /** Effective cost scaled by the cost factor (L2H: {@code max(1, round(cost * factor))}). */
    public final int getCost(double factor) {
        return Math.max(1, (int) Math.round(cost * factor));
    }

    public final int weight() {
        return weight;
    }

    public final int maxRank() {
        return maxRank;
    }

    public final int minLevel() {
        return minLevel;
    }

    /** Whether this trait may be rolled on the given mob (trait-side restriction). */
    public boolean appliesTo(LivingEntity mob) {
        return true;
    }

    /** Called once when the trait is granted (apply attributes / lasting effects). */
    public void initialize(LivingEntity mob, int rank) {
    }

    /** Periodic hook (no ticker is scheduled yet; reserved for future tick-based traits). */
    public void tick(LivingEntity mob, int rank) {
    }

    /** The mob dealt damage to {@code target}. */
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
    }

    /** The mob took damage. */
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
    }

    /** The mob took damage from {@code attacker} (the resolved living source, or null). */
    public void onAttackedBy(LivingEntity mob, int rank, @Nullable LivingEntity attacker,
                             EntityDamageByEntityEvent event) {
    }

    /** The mob died. */
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
    }

    /** A projectile this trait fired (via {@link DIV.enhancedMobs.trait.base.RangedTrait}) hit. */
    public void onProjectileHit(@Nullable LivingEntity shooter, int rank, Projectile projectile,
                                ProjectileHitEvent event) {
    }
}
