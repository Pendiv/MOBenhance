package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** プレイヤーの攻撃を確率でパリィし無効化する。確率はランクに比例。 */
public final class JustParryTrait extends Trait {

    public JustParryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("just_parry", "PARRY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player && ThreadLocalRandom.current().nextDouble() < 0.3 + 0.07 * rank) {
            event.setCancelled(true);
            // パリィされたことを音で伝える（原典に無い演出だが視認性向上のため追加）
            mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        }
    }
}
