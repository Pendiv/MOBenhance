package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * チェストプレートスキル「Adaptive（適応）」。特定の敵 1 体への被弾を重ねるごとに適応し、
 * 最大 {@link ItemSkills#ADAPTIVE_MAX_REDUCTION} までダメージを軽減する（増分は 1 発目ほど大きく漸近）。
 * 適応対象は常に最後に攻撃してきた敵 1 体のみ。別の敵に攻撃されると即座に対象が切替＝適応はリセット。
 */
public final class AdaptiveListener implements Listener {

    private record State(UUID mob, int hits) { }

    private final Map<UUID, State> states = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        int stage = ItemSkills.activeStage(chest, ItemSkills.SKILL_ADAPTIVE);
        if (stage < 0 || ItemEnhancer.isBroken(chest) || ItemSkills.armorSkillsLocked(player)) {
            return;
        }
        UUID attacker = attackerId(event);
        if (attacker == null) {
            return;
        }
        State st = states.get(player.getUniqueId());
        int n = ItemSkills.ADAPTIVE_HITS[stage];
        int hits = (st != null && st.mob().equals(attacker)) ? Math.min(st.hits() + 1, n) : 1;
        states.put(player.getUniqueId(), new State(attacker, hits));
        // 軽減 = max × (1 − (1 − 1/N)^hits)。増分が残り幅×(1/N) で減衰し N 発で 90% へ漸近。
        double reduction = ItemSkills.ADAPTIVE_MAX_REDUCTION * (1 - Math.pow(1 - 1.0 / n, hits));
        event.setDamage(event.getDamage() * (1 - reduction));
    }

    /** 攻撃元の識別子（投射物は射手）。 */
    private UUID attackerId(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof LivingEntity shooter) {
            return shooter.getUniqueId();
        }
        if (event.getDamager() instanceof LivingEntity living) {
            return living.getUniqueId();
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }
}
