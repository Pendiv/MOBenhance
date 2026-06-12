package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 夢に融ける — 致死ダメージを受けると死亡をキャンセルして「消滅の振り」
 * （透明・無音・AI停止・無敵・消火・デスポーン保留）に入り、(12-2×min(rank,3)) 秒後に復活する。
 * 回数無制限で、復活のたび攻撃力・最大HP +10×rank% × 復活回数を永続で積み増して全快する。
 * 偽死中に届いた致死（無敵貫通ダメージ）は素通しされ、本当に死ぬ。
 */
public final class DreamMeltTrait extends Trait {

    public DreamMeltTrait(int cost, int weight, int maxRank, int minLevel) {
        super("dream_melt", "DREAM", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return; // 致死のみ反応。
        }
        if (EntityState.hasFlag(mob, "dream_until")) {
            return; // 偽死中の死は素通し（原典の絶対殺害貫通に相当）。
        }
        event.setCancelled(true);
        mob.setHealth(1.0);

        // 消滅の振り: 透明・無音・無敵・AI停止・消火・デスポーン保留。
        mob.setInvisible(true);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setFireTicks(0);
        mob.setRemoveWhenFarAway(false);
        if (mob instanceof Mob asMob) {
            asMob.setTarget(null);
            asMob.setAI(false);
        }
        EntityState.setFlag(mob, "dream_until", 240 - 40 * Math.min(rank, 3));
        EntityState.setInt(mob, "dream_feign", 1);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "dream_feign", 0) == 0 || EntityState.hasFlag(mob, "dream_until")) {
            return;
        }
        // 復活: 振りを全解除し、復活回数に応じてステータスを積み増して全快。
        EntityState.setInt(mob, "dream_feign", 0);
        mob.setInvisible(false);
        mob.setSilent(false);
        mob.setInvulnerable(false);
        if (mob instanceof Mob asMob) {
            asMob.setAI(true);
        }

        int stacks = EntityState.addInt(mob, "dream_stacks", 1);
        double bonus = 0.10 * rank * stacks;
        EnhancedMobs plugin = EnhancedMobs.get();
        Mobs.addModifier(mob, Attribute.ATTACK_DAMAGE, new NamespacedKey(plugin, "trait_dream_atk"),
                bonus, AttributeModifier.Operation.ADD_SCALAR);
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, new NamespacedKey(plugin, "trait_dream_hp"),
                bonus, AttributeModifier.Operation.ADD_SCALAR);
        mob.setHealth(Mobs.maxHealth(mob));
    }
}
