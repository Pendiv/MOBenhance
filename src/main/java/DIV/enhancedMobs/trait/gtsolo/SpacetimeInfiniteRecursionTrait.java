package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/** 時空族: 死亡時に自身の半レベルのコピーを同地点に出現させる。レベルが1未満なら再帰しない。 */
public final class SpacetimeInfiniteRecursionTrait extends Trait {

    public SpacetimeInfiniteRecursionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_infinite_recursion", "STRECUR", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel < 1) {
            return;
        }
        Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
        if (copy instanceof LivingEntity living) {
            EnhancedMobs.get().initializeMob(living, childLevel);
        }
    }
}
