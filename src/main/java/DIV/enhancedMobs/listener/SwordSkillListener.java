package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 剣のレベリングスキル2種。
 * <ul>
 *   <li><b>勇猛果敢</b>: 敵を攻撃するたび攻撃力上昇（力）エフェクトを獲得。効果中に攻撃すると
 *       レベルが1ずつ上昇。上限 5/10/20/99、効果時間 5/5/6/10 秒（攻撃のたびリフレッシュ）。</li>
 *   <li><b>突飛</b>: 右クリックで前方へ吹き飛ぶ（突進距離は段階で上昇）。強化2回以降は道中の敵を
 *       攻撃力ぶん切りつける（爆風エフェクト = 剣戟表現）。CT 5/4.5/4/1.5 秒。</li>
 * </ul>
 */
public final class SwordSkillListener implements Listener {

    /** 突飛: 切りつけ判定の継続 tick。 */
    private static final int DASH_SLASH_TICKS = 12;

    private final EnhancedMobs plugin;

    public SwordSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    // ---- 攻撃時スキル（勇猛果敢・攻撃滞留） ----

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (ItemEnhancer.isBroken(held)) {
            return;
        }
        valor(player, held);
        linger(player, held, victim, event.getFinalDamage());
    }

    /** 勇猛果敢: 効果中の攻撃でレベル+1（上限あり）、切れていればレベル1から。 */
    private void valor(Player player, ItemStack held) {
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_VALOR);
        if (stage < 0) {
            return;
        }
        PotionEffect current = player.getPotionEffect(PotionEffectType.STRENGTH);
        int amplifier = current == null ? 0
                : Math.min(current.getAmplifier() + 1, ItemSkills.VALOR_CAP[stage] - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                ItemSkills.VALOR_DURATION_SEC[stage] * 20, amplifier, false, true, true));
    }

    /**
     * 攻撃滞留（付加スキル）: 稀に攻撃ダメージを記憶した滞留クラウドが発生。
     * クラウド内の生物（攻撃者本人を除く）へ1秒ごとに記憶ダメージを与える（3秒間）。
     */
    private void linger(Player player, ItemStack held, LivingEntity victim, double dealtDamage) {
        if (!ItemSkills.hasBonus(held, ItemSkills.BONUS_ATTACK_LINGER)) {
            return;
        }
        int stage = ItemSkills.bonusStage(held);
        if (ThreadLocalRandom.current().nextDouble() >= ItemSkills.LINGER_CHANCE[stage]) {
            return;
        }
        Location center = victim.getLocation();
        AreaEffectCloud cloud = center.getWorld().spawn(center, AreaEffectCloud.class, c -> {
            c.setParticle(Particle.DRAGON_BREATH);
            c.setRadius(2.0f);
            c.setRadiusOnUse(0f);
            c.setRadiusPerTick(0f);
            c.setDuration(60);
            c.setSource(player);
        });
        new BukkitRunnable() {
            private int pulses;

            @Override
            public void run() {
                if (++pulses > 3 || !cloud.isValid()) {
                    cancel();
                    return;
                }
                for (Entity entity : center.getWorld().getNearbyEntities(center, 2.0, 1.5, 2.0)) {
                    if (entity instanceof LivingEntity living && entity != player
                            && !(entity instanceof ArmorStand)) {
                        living.damage(dealtDamage, player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ---- 突飛 ----

    @EventHandler
    public void onDash(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_DASH);
        if (stage < 0) {
            return;
        }
        event.setCancelled(true);
        if (player.getCooldown(held.getType()) > 0) {
            return;
        }
        if (ItemEnhancer.isBroken(held)) {
            player.sendActionBar(Component.text("破壊寸前のため使用できません", NamedTextColor.RED));
            return;
        }
        player.setCooldown(held.getType(), ItemSkills.DASH_COOLDOWN_TICKS[stage]);
        Vector dir = player.getLocation().getDirection().normalize();
        double power = ItemSkills.DASH_POWER[stage];
        boolean onGround = player.isOnGround();
        if (!onGround) {
            // 空中ダッシュは減衰: 上方向なら40%、それ以外は75%（無限上昇・空中機動の抑制）
            power *= dir.getY() > 0.1 ? 0.40 : 0.75;
        }
        Vector velocity = dir.multiply(power);
        if (onGround && velocity.getY() < 0.2) {
            velocity.setY(0.2); // 地上では地面に突き刺さらないよう最低限浮かせる（空中は下方向ダッシュ可）
        }
        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.4f);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0, 1, 0), 1);
        if (stage >= 2) {
            slashAlongPath(player);
        }
    }

    /** 強化2回以降: 突進の道中、周囲の敵を1体につき1回だけ攻撃力ぶん切りつける。 */
    private void slashAlongPath(Player player) {
        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = atk != null ? atk.getValue() : 1.0;
        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (++ticks > DASH_SLASH_TICKS || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getNearbyEntities(1.6, 1.2, 1.6)) {
                    if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand)
                            && hit.add(entity.getUniqueId())) {
                        living.damage(damage, player);
                        // 爆風エフェクトを剣戟の表現として使う
                        player.getWorld().spawnParticle(Particle.EXPLOSION,
                                living.getLocation().add(0, 1, 0), 1);
                        player.getWorld().playSound(living.getLocation(),
                                Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
