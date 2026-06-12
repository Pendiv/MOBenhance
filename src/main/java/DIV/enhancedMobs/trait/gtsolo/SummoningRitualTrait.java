package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/** 死亡時、同種のより強大な敵を1体召喚する（自然相場レベル + 自身レベル×10%×rank）。 */
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
            EnhancedMobs plugin = EnhancedMobs.get();
            // 原典: 自然抽選レベル（スポーン地点の相場）に 自身レベル×10%×rank を上乗せ。
            int natural = plugin.mobBonus().apply(living.getType(),
                    plugin.difficulty().compute(living.getLocation()));
            int bonus = (int) Math.round(MobData.of(mob).getLevel() * 0.10 * rank);
            plugin.initializeMob(living, natural + bonus);
            // 召喚体がさらに儀式を持つと死ぬたびに無限強化するため、召喚後は除去する（安全装置）。
            plugin.traits().stripTrait(living, "summoning_ritual");
        }
    }
}
