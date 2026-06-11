package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 再解釈 REPRINT: 攻撃者の武器エンチャントレベル合計に比例して被ダメを軽減する
 * （エンチャント強化による優位を打ち消す）。
 */
public final class ReprintTrait extends Trait {

    private final double reductionPerEnchantLevel;

    public ReprintTrait(int cost, int weight, int maxRank, int minLevel, double reductionPerEnchantLevel) {
        super("reprint", "REPR", cost, weight, maxRank, minLevel);
        this.reductionPerEnchantLevel = reductionPerEnchantLevel;
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int levels = 0;
        for (int level : weapon.getEnchantments().values()) {
            levels += level;
        }
        if (levels <= 0) {
            return;
        }
        double reduced = event.getDamage() - reductionPerEnchantLevel * rank * levels;
        event.setDamage(Math.max(0, reduced));
    }
}
