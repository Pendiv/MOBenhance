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

    /** 同種の過密上限（分裂は世代連鎖するため、近傍が密ならスキップして指数増殖を防ぐ）。 */
    private static final int MAX_NEARBY = 12;
    private static final double NEARBY_RADIUS = 12.0;

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel < 1) {
            return;
        }
        EntityType type = mob.getType();
        // 近傍の同種数で残枠を決める（過密ならスポーンしない）。
        int nearby = 0;
        for (Entity e : mob.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)) {
            if (e.getType() == type) {
                nearby++;
            }
        }
        int budget = Math.min(copies, MAX_NEARBY - nearby);
        if (budget <= 0) {
            return;
        }
        Location loc = mob.getLocation();
        for (int i = 0; i < budget; i++) {
            Entity copy = mob.getWorld().spawnEntity(loc, type);
            if (copy instanceof LivingEntity living) {
                EnhancedMobs.get().initializeMob(living, childLevel);
            }
        }
    }
}
