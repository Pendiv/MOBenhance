package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * メイスのレベリングスキル「メガトンスマッシュ」。
 * メイスの叩きつけ（落下しながらの攻撃）が命中すると対象をスタンさせる。
 * <ul>
 *   <li>Mob: AI を一時停止して完全に行動不能化（持続後に復帰）。</li>
 *   <li>プレイヤー等: スロウ/弱体化/採掘速度低下で近似的に拘束。</li>
 *   <li>ボス（エンダードラゴン・ウィザー・ウォーデン・エルダーガーディアン）はスタン時間が25%に減少。</li>
 * </ul>
 * CT 15/14/13/9 秒、スタン 1.5/1.7/1.9/3.0 秒（強化段階 0〜3）。
 */
public final class MaceSkillListener implements Listener {

    private static final Set<EntityType> BOSSES = EnumSet.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

    private final EnhancedMobs plugin;
    /** Mob ごとの AI 復帰タスク。スタンの掛け直しで前のタスクを取り消し、早期復帰を防ぐ。 */
    private final Map<UUID, BukkitTask> aiRestores = new HashMap<>();

    public MaceSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmash(EntityDamageByEntityEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK
                || !(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() != Material.MACE) {
            return;
        }
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_MEGATON);
        if (stage < 0 || ItemEnhancer.isBroken(held)) {
            return;
        }
        // 叩きつけ（落下しながらの攻撃）のみ対象。クールタイム中は発動しない（通常攻撃は通す）。
        if (player.getFallDistance() < ItemSkills.MEGATON_MIN_FALL
                || player.getCooldown(Material.MACE) > 0) {
            return;
        }
        player.setCooldown(Material.MACE, ItemSkills.MEGATON_COOLDOWN_SEC[stage] * 20);
        int ticks = ItemSkills.MEGATON_STUN_TICKS[stage];
        if (BOSSES.contains(victim.getType())) {
            ticks = Math.max(1, (int) Math.round(ticks * ItemSkills.MEGATON_BOSS_MULT));
        }
        stun(victim, ticks);
    }

    /** 対象をスタンさせる（Mob は AI 停止、その他はデバフで近似）。 */
    private void stun(LivingEntity victim, int ticks) {
        World world = victim.getWorld();
        world.spawnParticle(Particle.CRIT, victim.getLocation().add(0, victim.getHeight() * 0.6, 0),
                24, 0.3, 0.4, 0.3, 0.25);
        world.spawnParticle(Particle.EXPLOSION, victim.getLocation(), 1);
        world.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.9f, 0.7f);

        // 見た目・近似拘束（プレイヤー含む全対象に付与）。amplifier は強めにして移動・攻撃を阻害。
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 6, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 9, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 9, false, true, true));

        if (victim instanceof Mob mob) {
            BukkitTask previous = aiRestores.remove(mob.getUniqueId());
            if (previous != null) {
                previous.cancel();
            }
            mob.setAI(false);
            BukkitTask restore = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                aiRestores.remove(mob.getUniqueId());
                if (mob.isValid()) {
                    mob.setAI(true);
                }
            }, ticks);
            aiRestores.put(mob.getUniqueId(), restore);
        }
    }
}
