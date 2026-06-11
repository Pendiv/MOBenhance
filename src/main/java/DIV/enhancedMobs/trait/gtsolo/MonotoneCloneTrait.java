package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** 一定間隔で自身の弱体クローンを湧かせる。 */
public final class MonotoneCloneTrait extends Trait {

    public MonotoneCloneTrait(int cost, int weight, int maxRank, int minLevel) {
        super("monotone_clone", "CLONE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        // ボス（ウィザー/ドラゴン/ウォーデン）は自己複製禁止 — 止まらない増殖を防ぐため。
        return !Mobs.isBoss(mob.getType());
    }

    private static final int MAX_NEARBY = 6;

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "clone_cd")) {
            return;
        }
        EntityState.setFlag(mob, "clone_cd", 200);
        // 個体数制限: 同種が一定数以上いれば生成しない（無限増殖防止）。
        long sameType = mob.getNearbyEntities(16, 16, 16).stream()
                .filter(e -> e.getType() == mob.getType())
                .count();
        if (sameType >= MAX_NEARBY) {
            return;
        }
        int childLevel = MobData.of(mob).getLevel() / 2;
        if (childLevel >= 1) {
            Entity copy = mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
            if (copy instanceof LivingEntity living) {
                EnhancedMobs.get().initializeMob(living, childLevel);
                // クローン自身はこのトレイトを持たせない — 指数的増殖防止。
                EnhancedMobs.get().traits().stripTrait(living, "monotone_clone");
            }
        }
    }
}
