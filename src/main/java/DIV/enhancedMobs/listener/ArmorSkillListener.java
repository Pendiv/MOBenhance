package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 防具のレベリングスキル「ジャストブロック」。
 * 攻撃を受けると一定確率（25/30/35/50%、段階 0〜3）で完全にブロックする。
 * 複数部位に付いていても効果は一様（最大段階のみ採用、確率は累積しない）。
 */
public final class ArmorSkillListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int best = -1;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            int stage = ItemSkills.activeStage(piece, ItemSkills.SKILL_JUST_BLOCK);
            if (stage >= 0 && !ItemEnhancer.isBroken(piece)) {
                best = Math.max(best, stage);
            }
        }
        if (best < 0 || ThreadLocalRandom.current().nextDouble() >= ItemSkills.JUST_BLOCK_CHANCE[best]) {
            return;
        }
        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        player.sendActionBar(Component.text("ジャストブロック！", NamedTextColor.AQUA));
    }
}
