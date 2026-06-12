package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * クリーパー限定: 死亡時に同地点へ新規クリーパーをスポーンさせる（1個体1回）。
 * 元個体は普通に死ぬ（ドロップ・XPあり）。新個体はスポーンイベント経由で
 * レベル・特性が新規抽選される（原典の natural init と同じ構図）。
 */
public final class SecondChanceTrait extends Trait {

    public SecondChanceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("second_chance", "2NDCHANCE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        // 安全装置: ボス種別は複製禁止、生涯1回限り
        if (Mobs.isBoss(mob.getType()) || EntityState.hasFlag(mob, "sc_used")) {
            return;
        }
        EntityState.setFlag(mob, "sc_used", Integer.MAX_VALUE);
        // spawnEntity(CUSTOM) は MobListener.onSpawn を通り、レベル・特性が自動で新規抽選される
        Entity revived = mob.getWorld().spawnEntity(mob.getLocation(), EntityType.CREEPER);
        if (revived instanceof LivingEntity living) {
            // 無限連鎖防止: 新個体からは本特性を除去（安全装置）
            EnhancedMobs.get().traits().stripTrait(living, "second_chance");
        }
    }
}
