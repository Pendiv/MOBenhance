package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 再解釈 DISPELL: 対象が装備するプロテクション系エンチャントのレベル合計が多いほど、
 * モブの与ダメが増加する（防具を貫通するイメージ）。フェザーフォーリングは対象外。
 */
public final class DispellTrait extends Trait {

    private final double damagePerEnchantLevel;

    public DispellTrait(int cost, int weight, int maxRank, int minLevel, double damagePerEnchantLevel) {
        super("dispell", "DISP", cost, weight, maxRank, minLevel);
        this.damagePerEnchantLevel = damagePerEnchantLevel;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player)) {
            return;
        }
        int levels = protectionLevels(player);
        if (levels <= 0) {
            return;
        }
        event.setDamage(event.getDamage() + damagePerEnchantLevel * rank * levels);
    }

    private int protectionLevels(Player player) {
        int sum = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) {
                continue;
            }
            var ench = armor.getEnchantments();
            sum += ench.getOrDefault(Enchantment.PROTECTION, 0);
            sum += ench.getOrDefault(Enchantment.FIRE_PROTECTION, 0);
            sum += ench.getOrDefault(Enchantment.BLAST_PROTECTION, 0);
            sum += ench.getOrDefault(Enchantment.PROJECTILE_PROTECTION, 0);
            // フェザーフォーリングとメンディングは意図的に除外。
        }
        return sum;
    }
}
