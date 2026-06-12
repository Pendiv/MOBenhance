package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

/**
 * クールダウン付きで射撃する特性の基底クラス（スケルトン系の特殊矢、GRENADE など）。
 * サブクラスは {@link #launch} で飛翔体の生成・照準を実装し、
 * 必要なら {@link #onProjectileHit} で着弾時の挙動を定義する。
 */
public abstract class RangedTrait extends Trait {

    private final int cooldownTicks;

    protected RangedTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                          int cooldownTicks) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public final void tick(LivingEntity mob, int rank) {
        if (!(mob instanceof Mob asMob)) {
            return;
        }
        LivingEntity target = asMob.getTarget();
        if (target == null) {
            return;
        }
        String cooldown = "cd_" + id();
        if (EntityState.hasFlag(mob, cooldown)) {
            return;
        }
        Projectile projectile = launch(asMob, target, rank);
        if (projectile != null) {
            TraitProjectiles.tag(projectile, id(), rank);
        }
        EntityState.setFlag(mob, cooldown, cooldownTicks(rank));
    }

    /** rank依存のクールダウン。既定はコンストラクタの固定値。原典がrankで短縮する場合は上書き。 */
    protected int cooldownTicks(int rank) {
        return cooldownTicks;
    }

    /** 飛翔体を生成・照準して返す（タグ付けされる）。発射しない場合は null を返す。 */
    protected abstract Projectile launch(Mob mob, LivingEntity target, int rank);
}
