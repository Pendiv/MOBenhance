package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Set;

/** 死亡時に同種モブをレベル半減でコピースポーンする（分裂可能な種別のみ）。 */
public final class SplitTrait extends Trait {

    private static final Set<EntityType> SPLITTABLE = Set.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.CREEPER, EntityType.SILVERFISH, EntityType.SLIME, EntityType.MAGMA_CUBE);

    private final int copies;

    public SplitTrait(int cost, int weight, int maxRank, int minLevel, int copies) {
        super("split", "SPLIT", cost, weight, maxRank, minLevel);
        this.copies = copies;
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return SPLITTABLE.contains(mob.getType());
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel < 1) {
            return;
        }
        Location loc = mob.getLocation();
        EntityType type = mob.getType();
        for (int i = 0; i < copies; i++) {
            Entity copy = mob.getWorld().spawnEntity(loc, type);
            if (copy instanceof LivingEntity living) {
                EnhancedMobs.get().initializeMob(living, childLevel);
            }
        }
    }
}
