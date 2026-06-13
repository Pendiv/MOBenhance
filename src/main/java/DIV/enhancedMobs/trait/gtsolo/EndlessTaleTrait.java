package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 致死ダメージ時に確率でフルHP復活する。初回確率は 50% + 22.15%×rank（rank3 で確定）。
 * 復活成功のたびに確率が ×0.8 − 5% に減衰し、0 以下になると以後復活しない。
 */
public final class EndlessTaleTrait extends Trait {

    public EndlessTaleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("endless_tale", "ENDLESS", cost, weight, maxRank, minLevel);
    }

    /** 蘇生CD（tick）。オーラ等の毎tickダメージで蘇生スパムするのを防ぐ。 */
    private static final int REVIVE_CD = 100;

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // 無敵貫通（/kill・奈落）と継続的な環境ダメージは対象外。直近に蘇生していたら再発火しない。
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID
                || Mobs.isEnvironmentalDoT(cause) || EntityState.hasFlag(mob, "revive_cd")) {
            return;
        }
        if (mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        double chance = EntityState.getDouble(mob, "et_chance", 0.50 + 0.2215 * rank);
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        EntityState.setFlag(mob, "revive_cd", REVIVE_CD);
        Mobs.playRevivalEffect(mob);
        // 減衰は成功時のみ（原典: currentChance = c×0.8 − 0.05）
        EntityState.setDouble(mob, "et_chance", chance * 0.8 - 0.05);
    }
}
