package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * レベリングスキル「範囲破壊」（ピッケル限定）。
 * 右クリックで視線の先のブロックを中心に立方体を一括破壊する。巨大なピッケルを振り下ろす演出付き。
 * 範囲 3³/5³/7³/13³、CT 6/7/8/12 秒（強化段階 0〜3）。表示サイズは段階に比例。
 */
public final class PickaxeSkillListener implements Listener {

    private static final double REACH = 6.0;

    private final EnhancedMobs plugin;

    public PickaxeSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAreaBreak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_AREA_BREAK);
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
        RayTraceResult ray = player.rayTraceBlocks(REACH);
        if (ray == null || ray.getHitBlock() == null) {
            player.sendActionBar(Component.text("破壊するブロックを狙ってください", NamedTextColor.YELLOW));
            return; // 空振りは CT を消費しない
        }
        player.setCooldown(held.getType(), ItemSkills.AREA_BREAK_COOLDOWN_SEC[stage] * 20);
        int size = ItemSkills.AREA_BREAK_SIZE[stage];
        new SmashTask(plugin, ray.getHitBlock(), held.clone(), size).runTaskTimer(plugin, 0L, 1L);
    }

    /** 巨大ピッケルを振り下ろし、着地フレームで立方体を一括破壊する。 */
    private static final class SmashTask extends BukkitRunnable {

        /** 振り上げて静止して見せる tick（ためのモーション）。 */
        private static final int WINDUP_TICK = 6;
        /** 振り下ろし完了（=破壊発生）までの tick。ゆっくり振ってプレイヤーに見せる。 */
        private static final int IMPACT_TICK = 18;
        private static final int END_TICK = 28;

        private final EnhancedMobs plugin;
        private final Block center;
        private final ItemStack pickaxe;
        private final int size;
        private final ItemDisplay display;
        private final float scale;
        private int age;

        SmashTask(EnhancedMobs plugin, Block center, ItemStack pickaxe, int size) {
            this.plugin = plugin;
            this.center = center;
            this.pickaxe = pickaxe;
            this.size = size;
            this.scale = Math.max(2.0f, size * 0.6f); // 表示サイズは範囲に比例
            World world = center.getWorld();
            Location spawn = center.getLocation().add(0.5, 1.5 + size * 0.4, 0.5);
            this.display = world.spawn(spawn, ItemDisplay.class, d -> {
                d.setItemStack(pickaxe);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setTeleportDuration(1);
                d.setInterpolationDuration(2);
                d.setPersistent(false);
            });
            pose(70); // 振り上げ姿勢
        }

        @Override
        public void run() {
            try {
                tick();
            } catch (Throwable t) {
                plugin.getLogger().warning("AreaBreak aborted: " + t);
                cleanup();
            }
        }

        private void tick() {
            age++;
            if (age <= WINDUP_TICK) {
                pose(70); // 振り上げて静止（プレイヤーに見せる溜め）
            } else if (age <= IMPACT_TICK) {
                // 振り上げ(70°) → 振り下ろし(-20°) へゆっくり補間。
                double progress = (age - WINDUP_TICK) / (double) (IMPACT_TICK - WINDUP_TICK);
                pose((float) (70 - 90 * progress));
            }
            if (age == IMPACT_TICK) {
                smash();
            }
            if (age >= END_TICK) {
                cleanup();
            }
        }

        /** 立方体内の壊せるブロックを一括破壊（ピッケルでドロップ＝幸運/シルク反映）。 */
        private void smash() {
            World world = center.getWorld();
            world.playSound(center.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
            world.spawnParticle(Particle.BLOCK, center.getLocation().add(0.5, 0.5, 0.5),
                    80, size * 0.4, size * 0.4, size * 0.4, center.getBlockData());
            int half = size / 2;
            for (int dx = -half; dx <= half; dx++) {
                for (int dy = -half; dy <= half; dy++) {
                    for (int dz = -half; dz <= half; dz++) {
                        Block b = center.getRelative(dx, dy, dz);
                        if (isBreakable(b)) {
                            b.breakNaturally(pickaxe);
                        }
                    }
                }
            }
        }

        /** 固体・非液体・破壊可能（硬度≥0、ベッドロック/バリア等を除外）のブロックか。 */
        private static boolean isBreakable(Block b) {
            return !b.getType().isAir() && !b.isLiquid()
                    && b.getType().isSolid() && b.getType().getHardness() >= 0;
        }

        /** ピッケルを X 軸まわりに angle 度だけ傾けて振り上げ/振り下ろしを表現。 */
        private void pose(float angleDeg) {
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf().rotateX((float) Math.toRadians(angleDeg)),
                    new Vector3f(scale),
                    new Quaternionf()));
        }

        private void cleanup() {
            if (display.isValid()) {
                display.remove();
            }
            cancel();
        }
    }
}
