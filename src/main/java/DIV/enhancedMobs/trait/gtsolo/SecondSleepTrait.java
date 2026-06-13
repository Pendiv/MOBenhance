package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 2度寝: 致死ダメージを一度だけ無効化して満HP復活し、
 * ランダム10〜100秒の間 invulnerable（うたた寝）になる。AIは動き続ける。
 */
public final class SecondSleepTrait extends Trait {

    /** 原典: 無敵時間 200〜2000 tick（10〜100秒）の一様乱数。 */
    private static final int DELAY_MIN = 200;
    private static final int DELAY_MAX = 2000;

    public SecondSleepTrait(int cost, int weight, int maxRank, int minLevel) {
        super("second_sleep", "2NDSLEEP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "ss_used") || mob.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        event.setCancelled(true);
        mob.setHealth(Mobs.maxHealth(mob));
        Mobs.playRevivalEffect(mob);
        int delay = DELAY_MIN + ThreadLocalRandom.current().nextInt(DELAY_MAX - DELAY_MIN + 1);
        mob.setInvulnerable(true);
        EntityState.setFlag(mob, "ss_sleep", delay);
        EntityState.setInt(mob, "ss_sleeping", 1);
        EntityState.setFlag(mob, "ss_used", Integer.MAX_VALUE);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // フラグ失効を検出して無敵を解除（チャンクアンロードを跨いでも安全）
        if (EntityState.getInt(mob, "ss_sleeping", 0) == 1 && !EntityState.hasFlag(mob, "ss_sleep")) {
            mob.setInvulnerable(false);
            EntityState.setInt(mob, "ss_sleeping", 0);
        }
    }
}
