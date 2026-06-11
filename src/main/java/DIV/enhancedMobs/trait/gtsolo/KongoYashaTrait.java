package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** プレイヤーからの攻撃を60%軽減するが、常に最大HPの微量(0.38%)を追加で受ける。 */
public final class KongoYashaTrait extends Trait {

    public KongoYashaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("kongo_yasha", "KONGO", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof Player) {
            event.setDamage(event.getDamage() * 0.4 + Mobs.maxHealth(mob) * 0.0038);
        }
    }
}
