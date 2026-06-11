package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/** 死亡時に自身より高レベルの同種Mobを召喚する。 */
public final class SummoningRitualTrait extends Trait {

    public SummoningRitualTrait(int cost, int weight, int maxRank, int minLevel) {
        super("summoning_ritual", "RITUAL", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        // ボスがこのトレイトを持つと無限強化ループが生まれるため除外。
        return !Mobs.isBoss(mob.getType());
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
        if (copy instanceof LivingEntity living) {
            EnhancedMobs.get().initializeMob(living, MobData.of(mob).getLevel() + 5 * rank);
            // 召喚体がさらに儀式を持つと死ぬたびに無限強化するため、召喚後は除去する。
            EnhancedMobs.get().traits().stripTrait(living, "summoning_ritual");
        }
    }
}
