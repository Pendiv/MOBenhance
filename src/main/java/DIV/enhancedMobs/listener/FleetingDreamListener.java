package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * チェストプレートスキル「須臾の夢」。被弾するたびに防御力参照の HP 回復を行う。
 * 累積回復量が最大 HP に達するまでは CT に入らず回復し続け、達した時点で CT 開始。
 */
public final class FleetingDreamListener implements Listener {

    private final EnhancedMobs plugin;
    private final Map<UUID, Double> healed = new HashMap<>();      // CT サイクル内の累積回復量
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();  // CT 終了 tick

    public FleetingDreamListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        int stage = ItemSkills.activeStage(chest, ItemSkills.SKILL_FLEETING);
        if (stage < 0 || ItemEnhancer.isBroken(chest) || ItemSkills.armorSkillsLocked(player)) {
            return;
        }
        long now = Bukkit.getCurrentTick();
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until != null && now < until) {
            return; // CT 中は回復しない
        }
        AttributeInstance armorAttr = player.getAttribute(Attribute.ARMOR);
        double armor = armorAttr != null ? armorAttr.getValue() : 0;
        if (armor <= 0) {
            return;
        }
        double heal = armor * ItemSkills.FLEETING_HEAL_PCT[stage];
        double maxHp = maxHealth(player);
        // ダメージ適用後に確実に乗せるため次 tick で回復。
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            player.setHealth(Math.min(maxHp, player.getHealth() + heal));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3, 0.0);
        });
        double cum = healed.merge(player.getUniqueId(), heal, Double::sum);
        if (cum >= maxHp) {
            healed.put(player.getUniqueId(), 0.0);
            cooldownUntil.put(player.getUniqueId(), now + ItemSkills.FLEETING_CT_SEC[stage] * 20L);
        }
    }

    private double maxHealth(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        return max != null ? max.getValue() : 20.0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        healed.remove(event.getPlayer().getUniqueId());
        cooldownUntil.remove(event.getPlayer().getUniqueId());
    }
}
