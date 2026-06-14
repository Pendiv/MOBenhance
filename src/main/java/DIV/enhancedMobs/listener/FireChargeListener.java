package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * 剣のレベリングスキル「ファイアチャージ」。
 * 右クリックで前方へファイアチャージ（仮想の飛翔体）を中速で放ち、着弾時に攻撃力を参照した
 * 物理ダメージを与える。延焼・地形破壊はしない。
 * CT 12/10.8/8/5 秒、ダメージ 攻撃力の 80/90/100/150%（強化段階 0〜3）。
 */
public final class FireChargeListener implements Listener {

    private final EnhancedMobs plugin;

    public FireChargeListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_FIRE_CHARGE);
        if (stage < 0) {
            return;
        }
        event.setCancelled(true);
        if (player.getCooldown(held.getType()) > 0) {
            return;
        }
        if (ItemEnhancer.isBroken(held)) {
            Lang.actionbar(player, "emob.skill.broken_use");
            return;
        }
        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = (atk != null ? atk.getValue() : 1.0) * ItemSkills.FIRE_CHARGE_DAMAGE_PCT[stage];
        player.setCooldown(held.getType(), ItemSkills.FIRE_CHARGE_COOLDOWN_TICKS[stage]);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.1f);
        new ChargeTask(plugin, player, damage).runTaskTimer(plugin, 1L, 1L);
    }

    /** 前方へ直進する飛翔体。生物に接触したらダメージ、固体ブロック/射程到達で霧散する。 */
    private static final class ChargeTask extends BukkitRunnable {

        private static final double MAX_RANGE = 40.0;
        private static final double HIT_RADIUS = 1.0;

        private final EnhancedMobs plugin;
        private final Player thrower;
        private final double damage;
        private final Vector direction;
        private final Location pos;
        private final ItemDisplay display;
        private double traveled;
        private boolean finished;

        ChargeTask(EnhancedMobs plugin, Player thrower, double damage) {
            this.plugin = plugin;
            this.thrower = thrower;
            this.damage = damage;
            Location eye = thrower.getEyeLocation();
            this.direction = eye.getDirection().normalize();
            this.pos = eye.clone().add(direction.clone().multiply(1.0));
            this.display = pos.getWorld().spawn(pos.clone().setDirection(direction), ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.FIRE_CHARGE));
                d.setTeleportDuration(1);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
            });
        }

        @Override
        public void run() {
            try {
                tick();
            } catch (Throwable t) {
                plugin.getLogger().warning("FireCharge aborted: " + t);
                cleanup();
            }
        }

        private void tick() {
            if (finished) {
                return;
            }
            if (!thrower.isOnline() || !display.isValid()) {
                cleanup();
                return;
            }
            LivingEntity hit = nearbyEnemy();
            if (hit != null) {
                hit.damage(damage, thrower); // 物理ダメージ（延焼なし）
                impact(true);
                return;
            }
            pos.add(direction.clone().multiply(ItemSkills.FIRE_CHARGE_SPEED));
            traveled += ItemSkills.FIRE_CHARGE_SPEED;
            if (pos.getBlock().getType().isSolid() || traveled >= MAX_RANGE) {
                impact(false);
                return;
            }
            display.teleport(pos.clone().setDirection(direction));
            display.setInterpolationDelay(0);
            pos.getWorld().spawnParticle(Particle.FLAME, pos, 3, 0.05, 0.05, 0.05, 0.01);
            pos.getWorld().spawnParticle(Particle.SMOKE, pos, 1, 0.02, 0.02, 0.02, 0.0);
        }

        /** 飛翔体の現在位置付近の生きた対象（攻撃者・防具立て以外）。 */
        private LivingEntity nearbyEnemy() {
            LivingEntity best = null;
            double bestSq = Double.MAX_VALUE;
            for (Entity e : pos.getWorld().getNearbyEntities(pos, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (e instanceof LivingEntity le && e != thrower && !(e instanceof ArmorStand)
                        && le.isValid() && !le.isDead()) {
                    double sq = e.getLocation().distanceSquared(pos);
                    if (sq < bestSq) {
                        bestSq = sq;
                        best = le;
                    }
                }
            }
            return best;
        }

        private void impact(boolean onEntity) {
            pos.getWorld().playSound(pos, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
            pos.getWorld().spawnParticle(onEntity ? Particle.LAVA : Particle.SMOKE, pos, 12, 0.2, 0.2, 0.2, 0.05);
            cleanup();
        }

        private void cleanup() {
            finished = true;
            if (display.isValid()) {
                display.remove();
            }
            cancel();
        }
    }
}
