package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
 * チェストプレートスキル「往昔の余燼」。
 * <ul>
 *   <li>常時ダメージ軽減（{@link ItemSkills#EMBER_DR}）。</li>
 *   <li>被ダメが最大HPの {@link ItemSkills#EMBER_CAP_PCT} を超えたら、その秒は上限までに抑え、
 *       超過分を翌秒以降へ繰り越して毎秒分割で受ける。</li>
 *   <li>分割の 1 秒分が現在HPを超える致死分割になったら、その分を無効化する（CT
 *       {@link ItemSkills#EMBER_NULLIFY_CT_SEC}）。</li>
 * </ul>
 */
public final class LingeringEmberListener implements Listener {

    private final Map<UUID, Double> pending = new HashMap<>();        // 繰り越し被ダメ
    private final Map<UUID, Long> nullifyCtUntil = new HashMap<>();    // 致死分割無効化の CT 終了 tick

    public LingeringEmberListener(EnhancedMobs plugin) {
        // 1 秒ごとに繰り越し分を適用。
        Bukkit.getScheduler().runTaskTimer(plugin, this::drain, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        int stage = ItemSkills.activeStage(chest, ItemSkills.SKILL_EMBER);
        if (stage < 0 || ItemEnhancer.isBroken(chest) || ItemSkills.armorSkillsLocked(player)) {
            return;
        }
        double dmg = event.getDamage() * (1 - ItemSkills.EMBER_DR[stage]); // 常時DR
        double cap = maxHealth(player) * ItemSkills.EMBER_CAP_PCT;
        if (dmg > cap) {
            event.setDamage(cap);
            pending.merge(player.getUniqueId(), dmg - cap, Double::sum);
        } else {
            event.setDamage(dmg);
        }
    }

    /** 繰り越し被ダメを毎秒、最大HPの25%ずつ適用する（致死分割はCTで無効化）。 */
    private void drain() {
        long now = Bukkit.getCurrentTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Double pend = pending.get(id);
            if (pend == null || pend <= 0.0) {
                continue;
            }
            int stage = ItemSkills.activeStage(player.getInventory().getChestplate(), ItemSkills.SKILL_EMBER);
            if (stage < 0) {
                pending.remove(id); // スキルを外した／壊れた → 繰り越し破棄
                continue;
            }
            double maxHp = maxHealth(player);
            double chunk = Math.min(pend, maxHp * ItemSkills.EMBER_CAP_PCT);
            double hp = player.getHealth();
            if (chunk >= hp) {
                Long until = nullifyCtUntil.get(id);
                if (until == null || now >= until) {
                    // 致死分割を無効化（CT 開始）。
                    nullifyCtUntil.put(id, now + ItemSkills.EMBER_NULLIFY_CT_SEC[stage] * 20L);
                    pending.put(id, pend - chunk);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            player.getLocation().add(0, 1, 0), 24, 0.4, 0.6, 0.4, 0.02);
                    continue;
                }
                // CT 中は無効化できない → そのまま受ける（致死あり）。
            }
            player.setHealth(Math.max(0.0, hp - chunk));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.01);
            double remain = pend - chunk;
            if (remain <= 0.0) {
                pending.remove(id);
            } else {
                pending.put(id, remain);
            }
        }
    }

    private double maxHealth(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        return max != null ? max.getValue() : 20.0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        nullifyCtUntil.remove(event.getPlayer().getUniqueId());
    }
}
