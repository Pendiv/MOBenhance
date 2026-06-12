package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.item.ItemEnhancer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** 武器・防具に XP を付与する。キル時はモブレベルに比例した大きな値、攻撃の命中・被弾時は微量。 */
public final class ItemXpListener implements Listener {

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        int xp = Math.max(1, MobData.of(dead).getLevel());
        grantWeapon(killer, xp);
        grantArmor(killer, xp);
    }

    /** 採掘でも手持ちの道具に微量 XP（瞬時破壊ブロックは対象外）。 */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().getHardness() > 0) {
            grantWeapon(event.getPlayer(), 1);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            grantWeapon(attacker, 1);
        }
        if (event.getEntity() instanceof Player victim) {
            grantArmor(victim, 1);
        }
    }

    private void grantWeapon(Player player, int xp) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (ItemEnhancer.isEnhanceable(weapon)) {
            ItemEnhancer.grantXp(weapon, xp);
            player.getInventory().setItemInMainHand(weapon);
        }
    }

    private void grantArmor(Player player, int xp) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] armor = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};
        for (ItemStack piece : armor) {
            if (ItemEnhancer.isEnhanceable(piece)) {
                ItemEnhancer.grantXp(piece, xp);
            }
        }
        inv.setHelmet(armor[0]);
        inv.setChestplate(armor[1]);
        inv.setLeggings(armor[2]);
        inv.setBoots(armor[3]);
    }
}
