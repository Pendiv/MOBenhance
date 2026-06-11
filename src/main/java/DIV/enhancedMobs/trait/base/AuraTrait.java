package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 定期範囲効果トレイトの基底クラス。毎 tick、対象種別に一致する近隣の生きているエンティティ全てに
 * {@link #affect} を適用する。ダメージオーラ、デバフオーラ、味方バフなどに対応する。
 */
public abstract class AuraTrait extends Trait {

    public enum TargetKind {
        PLAYERS, MOBS, ALL
    }

    private final double range;
    private final TargetKind targetKind;

    protected AuraTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                        double range, TargetKind targetKind) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.range = range;
        this.targetKind = targetKind;
    }

    @Override
    public final void tick(LivingEntity mob, int rank) {
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity target) || target == mob) {
                continue;
            }
            boolean isPlayer = target instanceof Player;
            if (targetKind == TargetKind.PLAYERS && !isPlayer) {
                continue;
            }
            if (targetKind == TargetKind.MOBS && isPlayer) {
                continue;
            }
            affect(mob, rank, target);
        }
    }

    protected abstract void affect(LivingEntity mob, int rank, LivingEntity target);
}
