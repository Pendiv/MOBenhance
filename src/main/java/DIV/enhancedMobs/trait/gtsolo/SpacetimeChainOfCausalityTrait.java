package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** 時空族: 攻撃命中時にランク×3%の確率で最寄りのプレイヤーの足元へテレポートする。 */
public final class SpacetimeChainOfCausalityTrait extends Trait {

    public SpacetimeChainOfCausalityTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_chain_of_causality", "STCHAIN", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (ThreadLocalRandom.current().nextDouble() < 0.03 * rank) {
            Player player = Mobs.nearestPlayer(mob, 32);
            if (player != null) {
                mob.teleport(player.getLocation());
            }
        }
    }
}
