package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 天邪鬼 — クリーパー専用。炎・爆発のダメージはそのまま受けるが、膨張を巻き戻して
 * 起爆させない。代わりに水に触れると強制起爆する。プレイヤー接近による通常の膨張AIは
 * 干渉しない（vanilla 動作維持）。
 */
public final class ContrarianTrait extends Trait {

    public ContrarianTrait(int cost, int weight, int maxRank, int minLevel) {
        super("contrarian", "CONTRA", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        if (!(mob instanceof Creeper creeper)) {
            return;
        }
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                // ダメージはキャンセルしない。膨張のみリセットして炎・爆発では起爆しない。
                if (creeper.isIgnited()) {
                    creeper.setIgnited(false);
                }
                creeper.setFuseTicks(0);
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 水接触で強制起爆（「水で爆発する」天邪鬼の代替経路）。
        if (mob instanceof Creeper creeper && creeper.isInWater()
                && creeper.getFuseTicks() <= 0 && !creeper.isIgnited()) {
            creeper.setIgnited(true);
        }
    }
}
