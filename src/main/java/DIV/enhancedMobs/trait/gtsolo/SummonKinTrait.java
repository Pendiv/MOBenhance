package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/** Creeper that periodically summons kin creepers. */
public final class SummonKinTrait extends Trait {

    public SummonKinTrait(int cost, int weight, int maxRank, int minLevel) {
        super("summon_kin", "SUMKIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "kin_cd")) {
            return;
        }
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel >= 1) {
            Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), EntityType.CREEPER);
            if (copy instanceof LivingEntity living) {
                EnhancedMobs.get().initializeMob(living, childLevel);
            }
        }
        EntityState.setFlag(mob, "kin_cd", 200);
    }
}
