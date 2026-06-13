package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * レベリングスキル「投擲」（槍限定）。
 * 右クリックで視線の先にいる敵をロックし、仮想の槍（ItemDisplay 表示のみ・実アイテムは消費しない）を
 * その敵へ飛ばす。槍が到達したタイミングで魔法的な追撃ダメージ（攻撃力 × 飛翔初速）を与える。
 * 視線の先に敵がいなければ前方へ飛んで消えるだけ（ダメージなし）。
 * CT 10/9/8/3 秒、初速 1.6/2.0/2.5/3.5（強化段階 0〜3）。
 */
public final class SpearThrowListener implements Listener {

    private static final double LOCK_RANGE = 48.0;

    private final EnhancedMobs plugin;

    public SpearThrowListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onThrow(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_THROW);
        if (stage < 0) {
            return;
        }
        event.setCancelled(true);
        if (player.getCooldown(held.getType()) > 0) {
            return;
        }
        if (ItemEnhancer.isBroken(held)) {
            player.sendActionBar(Component.text("破壊寸前のため投擲できません", NamedTextColor.RED));
            return;
        }
        double speed = ItemSkills.THROW_SPEED[stage];
        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = (atk != null ? atk.getValue() : 1.0) * speed; // 速いほど高威力

        // 視線の先にいる対象をロック（壁越しは無効）。突進と同様、敵に限らず生物全般を対象にする
        // （Enemy 限定だと検証用のパッシブmob等に当たらず「ダメージが出ない」原因になっていた）。
        Location eye = player.getEyeLocation();
        RayTraceResult ray = player.getWorld().rayTraceEntities(eye, eye.getDirection(), LOCK_RANGE, 1.0,
                e -> e instanceof LivingEntity && e != player && !(e instanceof ArmorStand));
        LivingEntity target = ray != null && ray.getHitEntity() instanceof LivingEntity le ? le : null;
        if (target != null && !player.hasLineOfSight(target)) {
            target = null;
        }

        player.setCooldown(held.getType(), ItemSkills.THROW_COOLDOWN_SEC[stage] * 20);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
        new ThrowTask(plugin, player, held.clone(), speed, damage, target).runTaskTimer(plugin, 1L, 1L);
    }

    /** ロックした敵へ槍をホーミングさせ、到達時にダメージを与える（敵なしなら前方へ飛んで消える）。 */
    private static final class ThrowTask extends BukkitRunnable {

        private static final double MAX_RANGE = 48.0;
        private static final int TIMEOUT_TICKS = 20 * 6;

        private final EnhancedMobs plugin;
        private final Player thrower;
        private final double speed;
        private final double damage;
        private final LivingEntity target;
        private final Vector direction;
        private final ItemDisplay display;
        private final Location pos;
        private double traveled;
        private int age;
        private boolean finished;

        ThrowTask(EnhancedMobs plugin, Player thrower, ItemStack spear, double speed, double damage,
                  LivingEntity target) {
            this.plugin = plugin;
            this.target = target;
            this.thrower = thrower;
            this.speed = speed;
            this.damage = damage;
            Location eye = thrower.getEyeLocation();
            this.direction = eye.getDirection().normalize();
            this.pos = eye.clone().add(0, -0.2, 0).add(direction.clone().multiply(1.0));
            this.display = pos.getWorld().spawn(pos.clone().setDirection(direction), ItemDisplay.class, d -> {
                d.setItemStack(spear);
                d.setTeleportDuration(1);
                d.setInterpolationDuration(1);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
            });
            orient();
        }

        @Override
        public void run() {
            try {
                tick();
            } catch (Throwable t) {
                plugin.getLogger().warning("SpearThrow aborted: " + t);
                cleanup();
            }
        }

        private void tick() {
            if (finished) {
                return;
            }
            if (++age > TIMEOUT_TICKS || !thrower.isOnline() || !display.isValid()) {
                cleanup();
                return;
            }
            // 当たり判定（毎tick）: 槍の位置付近の敵に当たればダメージ。ロックの成否に依存しない
            // ため、ロックや視線判定が外れても確実に当たる（突進・飛翔斧と同じ近接ダメージ方式）。
            LivingEntity hit = nearbyEnemy();
            if (hit != null) {
                hit.damage(damage, thrower); // 突進と同一の実証済みダメージ経路
                impact(hit.getLocation().add(0, hit.getHeight() * 0.5, 0), true);
                return;
            }
            // ロック中は体の中心へホーミング（当たり判定が拾うまで誘導）。
            if (target != null && target.isValid() && !target.isDead()) {
                Location aim = target.getLocation().add(0, target.getHeight() * 0.5, 0);
                Vector toTarget = aim.toVector().subtract(pos.toVector());
                if (toTarget.lengthSquared() > 1.0e-6) {
                    direction.copy(toTarget.normalize());
                }
            }
            pos.add(direction.clone().multiply(speed));
            traveled += speed;
            if (pos.getBlock().getType().isSolid() || traveled >= MAX_RANGE) {
                impact(pos.clone(), false);
                return;
            }
            display.teleport(pos.clone().setDirection(direction));
            display.setInterpolationDelay(0);
            orient();
            pos.getWorld().spawnParticle(Particle.CRIT, pos, 2, 0.05, 0.05, 0.05, 0.02);
        }

        /** 槍の現在位置付近の生きた対象（攻撃者・防具立て以外）。速いほど判定を広げてすり抜けを防ぐ。 */
        private LivingEntity nearbyEnemy() {
            double r = Math.max(1.2, speed * 0.6 + 0.8);
            LivingEntity best = null;
            double bestSq = Double.MAX_VALUE;
            for (Entity e : pos.getWorld().getNearbyEntities(pos, r, r, r)) {
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

        private void impact(Location at, boolean onEntity) {
            at.getWorld().playSound(at, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
            at.getWorld().spawnParticle(onEntity ? Particle.CRIT : Particle.ENCHANTED_HIT,
                    at, 8, 0.1, 0.1, 0.1, 0.05);
            cleanup();
        }

        /** 進行方向に沿って槍を寝かせる。視認性のため大きめに表示。 */
        private void orient() {
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf().rotateX((float) Math.toRadians(45)).rotateY((float) Math.toRadians(-90)),
                    new Vector3f(2.5f),
                    new Quaternionf()));
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
