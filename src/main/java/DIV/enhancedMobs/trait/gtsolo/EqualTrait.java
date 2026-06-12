package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * プレイヤーを新たにターゲットするたびに5秒間（100t）完全無敵になる。
 * 無敵窓中の再ターゲットでは延長せず、窓が切れた後の付け替えで再発動する（回数無制限）。
 */
public final class EqualTrait extends Trait {

    private static final int WINDOW_TICKS = 100;

    public EqualTrait(int cost, int weight, int maxRank, int minLevel) {
        super("equal", "EQUAL", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        ensureRegistered(mob);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // チャンク再ロード等で FastTick 登録が消えた場合の再登録
        ensureRegistered(mob);
    }

    /** 原典の LivingChangeTargetEvent 相当: 1tick周期でターゲット変化を検知する。 */
    private static void ensureRegistered(LivingEntity mob) {
        if (!(mob instanceof Mob asMob) || FastTick.isRegistered(mob, "equal")) {
            return;
        }
        FastTick.register(mob, "equal", new FastTick.Handler() {
            private UUID lastTarget;

            @Override
            public boolean tick() {
                if (!asMob.isValid()) {
                    return false;
                }
                LivingEntity target = asMob.getTarget();
                UUID id = target == null ? null : target.getUniqueId();
                if (Objects.equals(id, lastTarget)) {
                    return true;
                }
                lastTarget = id;
                // 新ターゲットがプレイヤーかつ無敵窓が切れていれば発動（窓中は延長しない）
                if (target instanceof Player && !EntityState.hasFlag(asMob, "equal_invuln")) {
                    EntityState.setFlag(asMob, "equal_invuln", WINDOW_TICKS);
                }
                return true;
            }
        });
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (EntityState.hasFlag(mob, "equal_invuln")) {
            event.setCancelled(true);
        }
    }
}
