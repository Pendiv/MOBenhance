package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.api.Vanilla;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * デスホライゾーン。所持モブの半径32m以内にプレイヤーが入ると発動し、1秒ごとに自身の足元へ
 * 「凋落の道」を残す（6+2N 秒持続。座標に正確に配置したバリアの ItemDisplay で表現）。
 * 凋落の道に触れているプレイヤーは、触れている間だけ防御力・ダメージ軽減・防具強度を失う（0に固定）。
 *
 * <p>シングルトン特性のため、道の管理と効果適用は静的に行う。{@link #init} で起動する周期タスクが
 * 期限切れの道を撤去し、道に触れているプレイヤーへ毎回 0 固定の transient（時限）を付け直す。
 * 時限なので道から離れれば自然に失効する。</p>
 */
public final class DeathHorizonTrait extends Trait {

    private static final double RANGE = 32.0;
    /** 道の設置間隔（tick）。 */
    private static final int PLACE_INTERVAL = 20;
    /** 管理タスクの周期（tick）。 */
    private static final int SWEEP_INTERVAL = 5;
    /** 道に触れていると見なす水平半径。 */
    private static final double TOUCH_RADIUS = 0.7;
    /** 0固定 transient の持続（tick）。道から離れるとこの時間で失効する。 */
    private static final int EFFECT_TICKS = 12;
    private static final String SOURCE = "enhancedmobs:trait/death_horizon";

    private static final List<Path> paths = new ArrayList<>();
    /** モブごとの最終設置 gameTime（1秒スロットル用）。 */
    private static final Map<UUID, Long> lastPlace = new HashMap<>();
    private static boolean started;

    public DeathHorizonTrait(int cost, int weight, int maxRank, int minLevel) {
        super("death_horizon", "HORIZON", cost, weight, maxRank, minLevel);
    }

    /** 道の管理・効果適用タスクを起動する（onEnable から1度だけ）。 */
    public static void init(EnhancedMobs plugin) {
        if (started) {
            return;
        }
        started = true;
        plugin.getServer().getScheduler().runTaskTimer(plugin, DeathHorizonTrait::sweep,
                SWEEP_INTERVAL, SWEEP_INTERVAL);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (!playerInRange(mob)) {
            return; // 半径32m以内にプレイヤーが居なければ発動しない
        }
        long now = mob.getWorld().getGameTime();
        Long last = lastPlace.get(mob.getUniqueId());
        if (last != null && now - last < PLACE_INTERVAL) {
            return; // 1秒に1回
        }
        lastPlace.put(mob.getUniqueId(), now);
        placePath(mob, rank, now);
    }

    private boolean playerInRange(LivingEntity mob) {
        double sq = RANGE * RANGE;
        Location at = mob.getLocation();
        for (Player p : mob.getWorld().getPlayers()) {
            if (isVictim(p) && p.getLocation().distanceSquared(at) <= sq) {
                return true;
            }
        }
        return false;
    }

    /** 足元の正確な座標へ凋落の道（バリアの ItemDisplay）を設置する。 */
    private void placePath(LivingEntity mob, int rank, long now) {
        Location loc = mob.getLocation();
        World world = loc.getWorld();
        ItemDisplay display = world.spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.BARRIER));
            d.setBrightness(new Display.Brightness(15, 15));
            d.setPersistent(false);
            d.setGlowing(true);
            d.setGlowColorOverride(Color.PURPLE);
            // 地面に薄く沿わせる（少し潰して足元へ）。
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.02f, 0f),
                    new Quaternionf(),
                    new Vector3f(1.0f, 0.1f, 1.0f),
                    new Quaternionf()));
        });
        long expiry = now + (6L + 2L * rank) * 20L;
        paths.add(new Path(world, loc.getX(), loc.getY(), loc.getZ(), expiry, display));
    }

    // ---- 静的管理 ----

    private static void sweep() {
        // 期限切れ・無効な道を撤去。
        Iterator<Path> it = paths.iterator();
        while (it.hasNext()) {
            Path p = it.next();
            boolean expired = p.display == null || !p.display.isValid()
                    || p.world.getGameTime() >= p.expiry;
            if (expired) {
                if (p.display != null && p.display.isValid()) {
                    p.display.remove();
                }
                it.remove();
            }
        }
        if (paths.isEmpty()) {
            lastPlace.clear(); // 道が無ければスロットル情報も不要（リーク防止）
            return;
        }
        // 道に触れているプレイヤーへ 0 固定を付け直す（時限なので離れれば失効）。
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVictim(player) && touchingPath(player)) {
                applyDecay(player);
            }
        }
    }

    private static boolean touchingPath(Player player) {
        Location at = player.getLocation();
        World world = at.getWorld();
        double rSq = TOUCH_RADIUS * TOUCH_RADIUS;
        for (Path p : paths) {
            if (p.world != world) {
                continue;
            }
            double dx = at.getX() - p.x;
            double dz = at.getZ() - p.z;
            double dy = at.getY() - p.y;
            if (dx * dx + dz * dz <= rSq && dy >= -0.5 && dy <= 1.2) {
                return true;
            }
        }
        return false;
    }

    /** 防御力・防具強度・各種ダメージ軽減を 0 に固定する（時限 transient、毎周期で付け直す）。 */
    private static void applyDecay(Player player) {
        Attributes.removeAll(player, SOURCE);
        Attributes.addTransient(player, Vanilla.ARMOR, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, Vanilla.ARMOR_TOUGHNESS, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.PHYSICAL_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.MAGIC_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.FIRE_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.LIGHTNING_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.DAMAGE_TAKEN, SOURCE, Operation.SET, 1.0, EFFECT_TICKS);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.05, 0.2, 0.0);
    }

    private static boolean isVictim(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    /** 設置済みの凋落の道1つ。 */
    private record Path(World world, double x, double y, double z, long expiry, ItemDisplay display) {
    }
}
