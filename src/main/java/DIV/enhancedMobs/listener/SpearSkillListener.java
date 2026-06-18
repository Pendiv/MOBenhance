package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import io.papermc.paper.event.entity.EntityLungeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 槍のレベリングスキル「突進軽減」。
 * 槍の突進（lunge）後、短時間（5秒）落下ダメージを無効化する。
 * さらに突進時に満腹度を 2/4/6/20 回復する（段階 0〜3）。
 */
public final class SpearSkillListener implements Listener {

    /** プレイヤー UUID → 落下無効の期限（エポックms）。 */
    private final Map<UUID, Long> fallImmune = new ConcurrentHashMap<>();

    /**
     * 槍エンチャントの突進（lunge）を無効化する。機動は牽引などのスキルへ集約する。
     * LOWEST で先にキャンセルするため、以降の {@link #onLunge}（突進軽減）も発火しない。
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void disableSpearLunge(EntityLungeEvent event) {
        if (event.getEntity() instanceof Player player
                && player.getInventory().getItemInMainHand().getType().name().endsWith("SPEAR")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLunge(EntityLungeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_CHARGE);
        if (stage < 0 || ItemEnhancer.isBroken(held) || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        fallImmune.put(player.getUniqueId(),
                System.currentTimeMillis() + ItemSkills.CHARGE_FALL_IMMUNE_TICKS * 50L);
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + ItemSkills.CHARGE_HUNGER[stage]));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Long until = fallImmune.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        if (System.currentTimeMillis() <= until) {
            event.setCancelled(true);
        } else {
            fallImmune.remove(player.getUniqueId());
        }
    }

    /** 突進後に落下せず抜けたプレイヤーの記録を残さない（メモリリーク防止）。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fallImmune.remove(event.getPlayer().getUniqueId());
    }
}
