package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Enemy;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * レベリングスキル「飛翔」（斧限定）。
 * 右クリックで斧を投擲する。ItemDisplay が縦回転（約1回転/秒）しながら約5ブロック/秒で直進し、
 * 敵に着弾すると投擲時の攻撃力ぶんのダメージを与えて帰還する。ブロック着弾・射程到達でも帰還。
 * CT 9/8/7/3 秒、射程 12/14/16/99 ブロック（強化段階 0〜3）。CT はバニラのアイテムクールダウン表示を使用。
 * 飛行中の例外・タイムアウト・ログアウトいずれの場合もアイテムは必ずインベントリ返却かドロップで保全される。
 */
public final class FlyingAxeListener implements Listener {

    private final EnhancedMobs plugin;

    public FlyingAxeListener(EnhancedMobs plugin) {
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
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_FLYING);
        if (stage < 0) {
            return;
        }
        event.setCancelled(true); // 樹皮剥ぎ等、斧の通常右クリック動作を抑止
        if (player.getCooldown(held.getType()) > 0) {
            return;
        }
        if (ItemEnhancer.isBroken(held)) {
            player.sendActionBar(Component.text("破壊寸前のため投擲できません", NamedTextColor.RED));
            return;
        }
        // 投擲時点の攻撃力（斧を持った状態の属性値）をダメージとして記録
        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = atk != null ? atk.getValue() : 1.0;

        player.setCooldown(held.getType(), ItemSkills.FLYING_COOLDOWN_SEC[stage] * 20);
        ItemStack axe = held.clone();
        player.getInventory().setItemInMainHand(null);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1f);
        new FlightTask(player, axe, ItemSkills.FLYING_RANGE[stage], damage).runTaskTimer(plugin, 1L, 1L);
    }

    /** 投擲1回ぶんの飛行制御。往路 → （着弾/射程）→ 復路 → 返却。 */
    private static final class FlightTask extends BukkitRunnable {

        /** 往路速度: 約7.5ブロック/秒。 */
        private static final double SPEED_OUT = 0.375;
        /** 復路速度: 約15ブロック/秒（移動中のプレイヤーにも追いつく）。 */
        private static final double SPEED_BACK = 0.75;
        /** 縦回転: 約1回転/秒 = 18°/tick。 */
        private static final float SPIN_PER_TICK = (float) Math.toRadians(18);
        /** 射程99 + 帰還を見込んだ保険（60秒）。 */
        private static final int TIMEOUT_TICKS = 20 * 60;

        private final Player thrower;
        private final ItemStack axe;
        private final double range;
        private final double damage;
        private final Vector direction;
        private final ItemDisplay display;
        private final Location pos;
        private double traveled;
        private boolean returning;
        private float spin;
        private int age;

        FlightTask(Player thrower, ItemStack axe, double range, double damage) {
            this.thrower = thrower;
            this.axe = axe;
            this.range = range;
            this.damage = damage;
            Location eye = thrower.getEyeLocation();
            this.direction = eye.getDirection().normalize();
            // 視線の真上に重ねると見えないので、少し下（手元寄り）から飛ばす
            this.pos = eye.clone().add(0, -0.3, 0).add(direction.clone().multiply(1.2));
            this.display = pos.getWorld().spawn(pos.clone().setDirection(direction), ItemDisplay.class, d -> {
                d.setItemStack(axe);
                d.setTeleportDuration(1);
                d.setInterpolationDuration(1);
                d.setBrightness(new Display.Brightness(15, 15)); // 夜・洞窟でも視認できる全光
                d.setPersistent(false);
            });
        }

        @Override
        public void run() {
            try {
                tick();
            } catch (Throwable t) {
                // どんな異常でもアイテムは失わせない
                EnhancedMobs.get().getLogger().warning("FlyingAxe flight aborted: " + t);
                recover();
            }
        }

        private void tick() {
            if (++age > TIMEOUT_TICKS || !thrower.isOnline() || thrower.isDead() || !display.isValid()) {
                recover();
                return;
            }
            if (!returning) {
                pos.add(direction.clone().multiply(SPEED_OUT));
                traveled += SPEED_OUT;
                if (hitEntity() || pos.getBlock().getType().isSolid() || traveled >= range) {
                    returning = true;
                }
            } else {
                Vector back = thrower.getEyeLocation().toVector().subtract(pos.toVector());
                if (back.length() < 2.0) {
                    finish();
                    return;
                }
                pos.add(back.normalize().multiply(SPEED_BACK));
            }
            spin += SPIN_PER_TICK;
            display.teleport(pos.clone().setDirection(direction));
            display.setInterpolationDelay(0);
            // rotateY(-90°): 進行方向と平行な縦の板（テクスチャ表裏は -90 側が正）。
            // その後 rotateX(+spin): 板の面内で後転方向に縦回転。
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf().rotateX(spin).rotateY((float) Math.toRadians(-90)),
                    new Vector3f(1.1f),
                    new Quaternionf()));
        }

        /** 最初に触れた敵対モブ1体へ投擲者起点のダメージを与える（ペット・プレイヤー・友好mobは巻き込まない）。 */
        private boolean hitEntity() {
            for (Entity entity : pos.getWorld().getNearbyEntities(pos, 0.7, 0.7, 0.7)) {
                if (entity instanceof Enemy && entity instanceof LivingEntity living && entity != thrower) {
                    living.damage(damage, thrower);
                    pos.getWorld().playSound(pos, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
                    return true;
                }
            }
            return false;
        }

        /** 投擲者へ返却（メインハンド優先 → インベントリ → 足元ドロップ）。 */
        private void finish() {
            giveBack();
            thrower.getWorld().playSound(thrower.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 1f);
            cleanup();
        }

        /** 異常終了時の保全: オンラインならインベントリへ直接返却、オフラインならその場にドロップ。 */
        private void recover() {
            if (thrower.isOnline()) {
                giveBack();
            } else {
                pos.getWorld().dropItemNaturally(pos, axe);
            }
            cleanup();
        }

        private void giveBack() {
            if (thrower.getInventory().getItemInMainHand().getType().isAir()) {
                thrower.getInventory().setItemInMainHand(axe);
                return;
            }
            for (ItemStack leftover : thrower.getInventory().addItem(axe).values()) {
                thrower.getWorld().dropItemNaturally(thrower.getLocation(), leftover);
            }
        }

        private void cleanup() {
            if (display.isValid()) {
                display.remove();
            }
            cancel();
        }
    }
}
