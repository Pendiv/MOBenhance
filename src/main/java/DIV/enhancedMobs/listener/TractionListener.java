package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 槍のレベリングスキル「牽引」。右クリックで前方をレイトレースし、壁（固体ブロック）があれば
 * その手前を錨として先に確定する。槍は距離に関わらず固定 0.4 秒で錨へ補間飛行し、着弾後 0.1 秒で
 * （計 0.5 秒）プレイヤー自身をその位置へ一気に引き寄せる（ワイヤーアクション）。
 * <ul>
 *   <li>飛行中にモブへ当たると攻撃力の100%の物理ダメージを与えてその場で終了（牽引なし）。</li>
 *   <li>壁が射程内に無ければ最大射程まで飛んで霧散（牽引なし）。</li>
 *   <li>錨は投擲時に確定するため、槍が物理的に刺さっていなくても牽引は進行する。</li>
 *   <li>着弾した槍は牽引が終わるまで表示され、骨粉エフェクト（HAPPY_VILLAGER）を放つ。牽引ルートは
 *       蝋燭の炎（SMALL_FLAME）で表示。</li>
 *   <li>飛行中・牽引中いつでも shift で中断でき、その瞬間の速度ベクトルは維持される。</li>
 *   <li>一連の動作中は落下ダメージを無効化する。</li>
 * </ul>
 * CT 固定 1.1 秒、射程 42/44/50/62（強化段階 0〜3）。
 */
public final class TractionListener implements Listener {

    /** 牽引の一連の動作中（飛行〜待機〜牽引）のプレイヤー。落下ダメージを無効化する。 */
    private static final Set<UUID> inAction = ConcurrentHashMap.newKeySet();

    private final EnhancedMobs plugin;

    public TractionListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    /** 牽引動作中のプレイヤーの落下ダメージを無効化する。 */
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && inAction.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_TRACTION);
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
        double damage = atk != null ? atk.getValue() : 1.0; // モブ命中時 100% 物理
        player.setCooldown(held.getType(), ItemSkills.TRACTION_COOLDOWN_TICKS);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
        new TractionTask(plugin, player, held.clone(), ItemSkills.TRACTION_RANGE[stage],
                ItemSkills.TRACTION_PULL_SPEED[stage], damage).runTaskTimer(plugin, 1L, 1L);
    }

    /** 投擲時に錨を確定し、固定時間で補間飛行 → 待機 → 牽引へと進む。 */
    private static final class TractionTask extends BukkitRunnable {

        private static final int PULL_TIMEOUT_TICKS = 40;
        private static final double ARRIVE_DISTANCE = 1.8;

        private enum Phase { FLYING, STICK_WAIT, PULLING }

        private final EnhancedMobs plugin;
        private final Player player;
        private final double pullSpeed;
        private final double damage;
        private final World world;
        private final Vector direction;
        private final Location start;
        private final Location target;
        /** 牽引先（壁が無ければ null＝牽引しない）。 */
        private final Location anchor;
        private final ItemDisplay spear;
        private final Location pos;
        private Phase phase = Phase.FLYING;
        private int flightTicks;
        private int waitTicks;
        private int pullTicks;
        private boolean armed;
        private boolean finished;

        TractionTask(EnhancedMobs plugin, Player player, ItemStack spearItem, double range,
                     double pullSpeed, double damage) {
            this.plugin = plugin;
            this.player = player;
            this.pullSpeed = pullSpeed;
            this.damage = damage;
            inAction.add(player.getUniqueId()); // 動作中は落下ダメージ無効

            Location eye = player.getEyeLocation();
            this.world = eye.getWorld();
            this.direction = eye.getDirection().normalize();
            this.start = eye.clone().add(0, -0.3, 0).add(direction.clone().multiply(1.2));

            // 投げた瞬間に方向を探索し、壁があれば錨を先に確定する。
            RayTraceResult ray = world.rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, true);
            if (ray != null && ray.getHitBlock() != null) {
                Vector hp = ray.getHitPosition();
                this.anchor = new Location(world, hp.getX(), hp.getY(), hp.getZ())
                        .subtract(direction.clone().multiply(0.2)); // 壁面の少し手前（空気側）
                this.target = anchor.clone();
            } else {
                this.anchor = null;
                this.target = eye.clone().add(direction.clone().multiply(range));
            }

            this.pos = start.clone();
            this.spear = world.spawn(pos.clone().setDirection(direction), ItemDisplay.class, d -> {
                d.setItemStack(spearItem);
                d.setTeleportDuration(1);
                d.setInterpolationDuration(1);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
                d.setTransformation(new Transformation(
                        new Vector3f(),
                        new Quaternionf().rotateX((float) Math.toRadians(45)).rotateY((float) Math.toRadians(-90)),
                        new Vector3f(2.5f),
                        new Quaternionf()));
            });
        }

        @Override
        public void run() {
            try {
                if (finished) {
                    return;
                }
                if (!player.isOnline() || player.isDead() || !spear.isValid()) {
                    cleanup();
                    return;
                }
                // shift で中断（速度ベクトルは維持＝この tick では何もしないので直前の velocity が残る）。
                if (!player.isSneaking()) {
                    armed = true;
                } else if (armed) {
                    cleanup();
                    return;
                }
                switch (phase) {
                    case FLYING -> fly();
                    case STICK_WAIT -> stickWait();
                    case PULLING -> pull();
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Traction aborted: " + t);
                cleanup();
            }
        }

        private void fly() {
            flightTicks++;
            double t = Math.min(1.0, flightTicks / (double) ItemSkills.TRACTION_FLIGHT_TICKS);
            Location prev = pos.clone();
            pos.setX(start.getX() + (target.getX() - start.getX()) * t);
            pos.setY(start.getY() + (target.getY() - start.getY()) * t);
            pos.setZ(start.getZ() + (target.getZ() - start.getZ()) * t);

            // モブ命中: この tick の移動区間をレイトレース（高速でもすり抜けない）。
            LivingEntity hit = segmentEnemy(prev, pos);
            if (hit != null) {
                hit.damage(damage, player); // モブ命中: 100% 物理ダメージを与えて終了
                world.playSound(pos, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
                world.spawnParticle(Particle.CRIT, pos, 12, 0.2, 0.2, 0.2, 0.1);
                cleanup();
                return;
            }
            spear.teleport(pos.clone().setDirection(direction));
            spear.setInterpolationDelay(0);
            world.spawnParticle(Particle.CRIT, pos, 2, 0.04, 0.04, 0.04, 0.01);

            if (flightTicks >= ItemSkills.TRACTION_FLIGHT_TICKS) {
                if (anchor != null) {
                    phase = Phase.STICK_WAIT;
                    world.playSound(anchor, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 0.8f);
                    world.spawnParticle(Particle.CRIT, anchor, 14, 0.2, 0.2, 0.2, 0.1);
                } else {
                    world.playSound(pos, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.2f); // 壁なし → 霧散
                    cleanup();
                }
            }
        }

        private void stickWait() {
            stuckSpearEffect();
            if (++waitTicks >= ItemSkills.TRACTION_STICK_WAIT_TICKS) {
                phase = Phase.PULLING;
                world.playSound(anchor, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.1f);
            }
        }

        private void pull() {
            stuckSpearEffect();
            if (++pullTicks > PULL_TIMEOUT_TICKS) {
                cleanup();
                return;
            }
            Location from = player.getEyeLocation();
            Vector toAnchor = anchor.toVector().subtract(from.toVector());
            double dist = toAnchor.length();
            if (dist < ARRIVE_DISTANCE) {
                player.setFallDistance(0f);
                world.playSound(anchor, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.3f);
                world.spawnParticle(Particle.GUST, anchor, 1);
                cleanup();
                return;
            }
            Vector velocity = toAnchor.normalize().multiply(pullSpeed);
            velocity.setY(velocity.getY() + 0.15); // 引っかかり防止に少し浮かせる
            player.setVelocity(velocity);
            player.setFallDistance(0f);
            candleRoute(from, anchor); // 牽引ルートを蝋燭の炎で表示
            player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0, 1, 0), 1);
        }

        /** 着弾した槍の演出: 骨粉を作物に使ったときのエフェクト（HAPPY_VILLAGER）。 */
        private void stuckSpearEffect() {
            world.spawnParticle(Particle.HAPPY_VILLAGER, anchor, 4, 0.25, 0.25, 0.25, 0.0);
        }

        /** 牽引ルートを蝋燭の炎（SMALL_FLAME）で結ぶ。 */
        private void candleRoute(Location from, Location to) {
            Vector delta = to.toVector().subtract(from.toVector());
            double length = delta.length();
            if (length < 1.0e-3) {
                return;
            }
            Vector step = delta.normalize().multiply(0.5);
            Location point = from.clone();
            for (double d = 0; d < length; d += 0.5) {
                world.spawnParticle(Particle.SMALL_FLAME, point, 1, 0, 0, 0, 0);
                point.add(step);
            }
        }

        /** 移動区間 [from→to] 上で最初に当たる生きた対象（攻撃者・防具立て以外）。 */
        private LivingEntity segmentEnemy(Location from, Location to) {
            Vector seg = to.toVector().subtract(from.toVector());
            double len = seg.length();
            if (len < 1.0e-4) {
                return null;
            }
            RayTraceResult ray = world.rayTraceEntities(from, seg.normalize(), len, 0.6,
                    e -> e instanceof LivingEntity && e != player && !(e instanceof ArmorStand));
            return ray != null && ray.getHitEntity() instanceof LivingEntity le
                    && le.isValid() && !le.isDead() ? le : null;
        }

        private void cleanup() {
            finished = true;
            inAction.remove(player.getUniqueId());
            if (spear.isValid()) {
                spear.remove();
            }
            cancel();
        }
    }
}
