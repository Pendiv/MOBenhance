package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 弓のレベリングと「正鵠を射る」スキル。
 * <ul>
 *   <li>発射ごとに弓へ少量 XP（レベリングの進行源）。</li>
 *   <li>レベルに応じて射撃ダメージ上昇（Lv あたり {@link ItemSkills#BOW_DAMAGE_PER_LEVEL}）。</li>
 *   <li>正鵠を射る: {@link ItemSkills#BULLSEYE_DRAW_TICKS} 以上ためて引くと、その射撃ダメージが
 *       段階に応じ +40/50/60/100%。</li>
 * </ul>
 * 破壊寸前の弓は性能 0 として強化を一切載せない。
 */
public final class BowSkillListener implements Listener {

    private final EnhancedMobs plugin;
    /** プレイヤーごとの引き始め tick（正鵠を射るの溜め時間判定用）。 */
    private final Map<UUID, Integer> drawStart = new HashMap<>();

    public BowSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    /** 弓を引き始めた tick を記録する。 */
    @EventHandler
    public void onDraw(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.BOW) {
            drawStart.put(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    /** 引きかけのまま抜けたプレイヤーの記録を残さない（メモリリーク防止）。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        drawStart.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.BOW || !ItemEnhancer.isEnhanceable(bow)) {
            return;
        }
        Integer start = drawStart.remove(player.getUniqueId());
        boolean broken = ItemEnhancer.isBroken(bow);

        // 射撃ダメージ強化（レベル + 正鵠を射る）。破壊寸前は載せない。
        if (!broken && event.getProjectile() instanceof AbstractArrow arrow) {
            int level = bow.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(ItemEnhancer.LEVEL, PersistentDataType.INTEGER, 1);
            double mult = 1.0 + level * ItemSkills.BOW_DAMAGE_PER_LEVEL;
            int stage = ItemSkills.activeStage(bow, ItemSkills.SKILL_BULLSEYE);
            boolean bullseye = stage >= 0 && start != null
                    && !ItemSkills.weaponSkillsLocked(player)
                    && Bukkit.getCurrentTick() - start >= ItemSkills.BULLSEYE_DRAW_TICKS;
            if (bullseye) {
                mult += ItemSkills.BULLSEYE_DAMAGE_PCT[stage];
                Location loc = player.getEyeLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.6f);
                loc.getWorld().spawnParticle(Particle.CRIT, loc.add(loc.getDirection()), 18, 0.1, 0.1, 0.1, 0.15);
            }
            arrow.setDamage(arrow.getDamage() * mult);
        }

        // 発射でレベリング XP（バニラの耐久処理後に確定させるため次 tick）。
        EquipmentSlot hand = event.getHand();
        Bukkit.getScheduler().runTask(plugin, () -> grantShotXp(player, hand));
    }

    private void grantShotXp(Player player, EquipmentSlot hand) {
        ItemStack cur = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (cur.getType() != Material.BOW || !ItemEnhancer.isEnhanceable(cur)) {
            return;
        }
        ItemEnhancer.grantXp(cur, 1);
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(cur);
        } else {
            player.getInventory().setItemInMainHand(cur);
        }
    }
}
