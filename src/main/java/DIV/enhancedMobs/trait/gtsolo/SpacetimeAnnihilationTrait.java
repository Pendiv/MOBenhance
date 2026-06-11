package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** 時空族: 致死ダメージを与えた対象のトーテム復活を封じる。 */
public final class SpacetimeAnnihilationTrait extends Trait {

    public SpacetimeAnnihilationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_annihilation", "STANNI", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target.getHealth() - event.getFinalDamage() <= 0) {
            EntityState.setFlag(target, "deny_resurrect", 40);
        }
    }
}
