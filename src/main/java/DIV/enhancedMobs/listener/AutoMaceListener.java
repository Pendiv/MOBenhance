package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Enemy;
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
import org.bukkit.util.Vector;

/**
 * 付加スキル「オートメイス」（メイス限定、付与アイテム = ヘビーコア）。
 * メイスを持ってスニーク右クリック → 視線の先の最も近い敵（壁越し無効）をロックオンし、
 * ろうそくの炎エフェクトを対象の周囲に円形表示。メイスはプレイヤーから対象へ低空でゆっくり接近し、
 * 対象の近くで真上へ大ジャンプ（10/12/14/25 ブロック）→ 急降下して衝突 → 高速で帰還する。
 * CT 10/9/8/2 秒（段階 0〜3）。ダメージ = 投擲時の攻撃力 + バニラ式スマッシュ落下ボーナス（密度エンチャ対応）。
 */
public final class AutoMaceListener implements Listener {

    private static final double TARGET_RANGE = 24.0;

    private final EnhancedMobs plugin;

    public AutoMaceListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMark(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemSkills.hasBonus(held, ItemSkills.BONUS_AUTO_MACE)) {
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
        LivingEntity target = findTarget(player);
        if (target == null) {
            player.sendActionBar(Component.text("視線の先に対象がいません", NamedTextColor.GRAY));
            return;
        }
        int stage = ItemSkills.bonusStage(held);
        double rise = ItemSkills.AUTO_MACE_RISE[stage];
        // 基礎ダメージは攻撃力属性のみ。落下ボーナスは着弾時に実落下距離からバニラ式で加算する
        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = atk != null ? atk.getValue() : 1.0;

        player.setCooldown(held.getType(), ItemSkills.AUTO_MACE_COOLDOWN_SEC[stage] * 20);
        ItemStack mace = held.clone();
        player.getInventory().setItemInMainHand(null);
        markRing(target); // ロックオン表示
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.6f);
        player.sendActionBar(Component.text("対象を指定しました: " + target.getType(), NamedTextColor.GOLD));
        new SmashTask(player, mace, target, rise, damage).runTaskTimer(plugin, 1L, 1L);
    }

    /** 視線の先の最も近い敵。ブロックが先に当たる（壁越し）場合は無効。 */
    private static LivingEntity findTarget(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(
                eye, eye.getDirection(), TARGET_RANGE, FluidCollisionMode.NEVER, true, 0.4,
                e -> e instanceof Enemy && e != player);
        if (result == null || !(result.getHitEntity() instanceof LivingEntity living)) {
            return null; // 何も無い、またはブロックが先に当たった
        }
        return living;
    }

    /** ろうそくの炎を対象の周囲に円形配置。 */
    private static void markRing(LivingEntity target) {
        World world = target.getWorld();
        Location center = target.getLocation();
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12;
            world.spawnParticle(Particle.SMALL_FLAME,
                    center.getX() + Math.cos(angle), center.getY() + 0.2, center.getZ() + Math.sin(angle),
                    1, 0, 0, 0, 0);
        }
    }

    /** 1回の自動攻撃: プレイヤーから対象へ低空接近 → 対象付近で大ジャンプ → 衝突 → 高速帰還。 */
    private static final class SmashTask extends BukkitRunnable {

        /** 接近はゆっくり見せる: 約6ブロック/秒。 */
        private static final double SPEED_APPROACH = 0.3;
        /** 大ジャンプ: 約18ブロック/秒で一気に駆け上がる。 */
        private static final double SPEED_JUMP = 0.9;
        /** 降下スマッシュ: 約36ブロック/秒。 */
        private static final double SPEED_DIVE = 1.8;
        /** 帰還はかなり速く: 約30ブロック/秒。 */
        private static final double SPEED_BACK = 1.5;
        /** この水平距離まで近づいたらジャンプ開始。 */
        private static final double JUMP_TRIGGER_DIST = 3.5;
        private static final int TIMEOUT_TICKS = 20 * 30;

        private static final int PHASE_APPROACH = 0;
        private static final int PHASE_JUMP = 1;
        private static final int PHASE_DIVE = 2;
        private static final int PHASE_RETURN = 3;

        private final Player thrower;
        private final ItemStack mace;
        private final LivingEntity target;
        private final double riseBlocks;
        private final double damage;
        private final ItemDisplay display;
        private final Location pos;
        private int phase = PHASE_APPROACH;
        private double diveStartY;
        private int age;

        SmashTask(Player thrower, ItemStack mace, LivingEntity target, double riseBlocks, double damage) {
            this.thrower = thrower;
            this.mace = mace;
            this.target = target;
            this.riseBlocks = riseBlocks;
            this.damage = damage;
            this.pos = thrower.getEyeLocation().add(0, 0.5, 0);
            this.display = pos.getWorld().spawn(pos, ItemDisplay.class, d -> {
                d.setItemStack(mace);
                d.setTeleportDuration(1);
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
                EnhancedMobs.get().getLogger().warning("AutoMace flight aborted: " + t);
                recover();
            }
        }

        private void tick() {
            if (++age > TIMEOUT_TICKS || !thrower.isOnline() || thrower.isDead() || !display.isValid()) {
                recover();
                return;
            }
            switch (phase) {
                case PHASE_APPROACH -> {
                    if (!alive(target)) {
                        phase = PHASE_RETURN;
                        break;
                    }
                    // 対象の胸元の高さへ向かって低空でゆっくり接近
                    Location aim = target.getLocation().add(0, 1.2, 0);
                    Vector dir = aim.toVector().subtract(pos.toVector());
                    Vector flat = dir.clone().setY(0);
                    if (flat.length() <= JUMP_TRIGGER_DIST) {
                        phase = PHASE_JUMP;
                        pos.getWorld().playSound(pos, Sound.ITEM_MACE_SMASH_AIR, 1f, 1.2f);
                        break;
                    }
                    pos.add(dir.normalize().multiply(SPEED_APPROACH));
                }
                case PHASE_JUMP -> {
                    if (!alive(target)) {
                        phase = PHASE_RETURN;
                        break;
                    }
                    // 対象の真上 riseBlocks まで大ジャンプ
                    Location apex = target.getLocation().add(0, riseBlocks, 0);
                    Vector dir = apex.toVector().subtract(pos.toVector());
                    if (dir.length() <= SPEED_JUMP || pos.getY() >= apex.getY()) {
                        phase = PHASE_DIVE;
                        diveStartY = pos.getY(); // 落下ボーナス計算の起点
                        break;
                    }
                    pos.add(dir.normalize().multiply(SPEED_JUMP));
                }
                case PHASE_DIVE -> {
                    if (!alive(target)) {
                        phase = PHASE_RETURN;
                        break;
                    }
                    Location aim = target.getLocation().add(0, target.getHeight() * 0.5, 0);
                    Vector dir = aim.toVector().subtract(pos.toVector());
                    if (dir.length() <= SPEED_DIVE) {
                        smash();
                        break;
                    }
                    pos.add(dir.normalize().multiply(SPEED_DIVE));
                }
                case PHASE_RETURN -> {
                    Vector back = thrower.getEyeLocation().toVector().subtract(pos.toVector());
                    if (back.length() < 2.0) {
                        finish();
                        return;
                    }
                    pos.add(back.normalize().multiply(SPEED_BACK));
                }
                default -> {
                }
            }
            if (phase != PHASE_RETURN && age % 2 == 0 && alive(target)) {
                markRing(target); // 攻撃中はロックオン円を維持
            }
            display.teleport(pos);
        }

        private static boolean alive(LivingEntity target) {
            return target.isValid() && !target.isDead();
        }

        /** 着弾: 攻撃力 + バニラ式落下ボーナス + 演出 → 帰還へ。 */
        private void smash() {
            double fall = Math.max(0, diveStartY - pos.getY());
            target.damage(damage + smashBonus(fall), thrower);
            World world = pos.getWorld();
            world.playSound(pos, Sound.ITEM_MACE_SMASH_GROUND, 1f, 1f);
            world.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 0.5, 0), 1);
            phase = PHASE_RETURN;
        }

        /**
         * バニラのメイススマッシュ式: 最初の3ブロック +4/個 → 次の5ブロック +2/個 → 以降 +1/個。
         * 密度エンチャント: さらに +0.5 × レベル × 落下ブロック。
         */
        private double smashBonus(double fall) {
            double bonus;
            if (fall <= 3) {
                bonus = fall * 4;
            } else if (fall <= 8) {
                bonus = 12 + (fall - 3) * 2;
            } else {
                bonus = 22 + (fall - 8);
            }
            int density = mace.getEnchantmentLevel(Enchantment.DENSITY);
            return bonus + 0.5 * density * fall;
        }

        /** 投擲者へ返却（メインハンド優先 → インベントリ → 足元ドロップ）。 */
        private void finish() {
            giveBack();
            thrower.getWorld().playSound(thrower.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
            cleanup();
        }

        /** 異常終了時の保全: オンラインならインベントリへ直接返却、オフラインならその場にドロップ。 */
        private void recover() {
            if (thrower.isOnline()) {
                giveBack();
            } else {
                pos.getWorld().dropItemNaturally(pos, mace);
            }
            cleanup();
        }

        private void giveBack() {
            if (thrower.getInventory().getItemInMainHand().getType().isAir()) {
                thrower.getInventory().setItemInMainHand(mace);
                return;
            }
            for (ItemStack leftover : thrower.getInventory().addItem(mace).values()) {
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
