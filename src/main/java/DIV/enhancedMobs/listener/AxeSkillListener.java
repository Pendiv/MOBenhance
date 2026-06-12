package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * 斧のレベリングスキル「たけのこ魔法」。
 * 攻撃すると敵を真上に吹き飛ばし（6/8/10/16 ブロック、段階 0〜3）、
 * 対象のいた足元にたけのこ（竹の苗）を出現させる。
 * （オールインはステータス変更のみのためリスナー不要。）
 */
public final class AxeSkillListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_TAKENOKO);
        if (stage < 0 || ItemEnhancer.isBroken(held)) {
            return;
        }
        // 高さ h まで打ち上がる初速 v ≈ √(0.16h)（MC の重力 0.08/tick² 近似）
        double height = ItemSkills.TAKENOKO_HEIGHT[stage];
        Vector velocity = victim.getVelocity();
        velocity.setY(Math.sqrt(0.16 * height));
        victim.setVelocity(velocity);

        Block feet = victim.getLocation().getBlock();
        if (feet.getType().isAir()) {
            feet.setType(Material.BAMBOO_SAPLING);
            feet.getWorld().playSound(feet.getLocation(), Sound.BLOCK_BAMBOO_SAPLING_PLACE, 1f, 1f);
        }
    }
}
