package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.api.Vanilla;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * デスホライゾーン。所持モブの半径32m以内にプレイヤーが入ると発動し、1秒ごとに自身の足元へ
 * 「凋落の道」を残す（6+2N 秒持続。座標へ BLOCK_MARKER バリアパーティクルで表示）。
 * 凋落の道に触れているプレイヤーは、触れている間だけ防御力・防具強度・各種ダメージ軽減を失い（0固定）、
 * さらに最大HPと攻撃力が本来の {@link #STAT_MULT}（80%）に引き下げられる。
 *
 * <p>シングルトン特性のため、道の管理と効果適用は静的に行う。{@link #init} で起動する周期タスクが
 * 期限切れの道を撤去しつつ生存中の道を毎周期描画し、道に触れているプレイヤーへ時限 transient の
 * デバフを付け直す。時限なので道から離れれば自然に失効する。</p>
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
    /** 道に触れている間の HP・攻撃力の倍率（本来の 80%）。 */
    private static final double STAT_MULT = 0.8;
    private static final String SOURCE = "enhancedmobs:trait/death_horizon";
    /** 道の表示に使うバリアのブロックデータ（BLOCK_MARKER 用）。 */
    private static final BlockData BARRIER_DATA = Material.BARRIER.createBlockData();

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

    /** 足元の座標へ凋落の道を1つ記録する（見た目は sweep で BLOCK_MARKER バリアパーティクルとして描画）。 */
    private void placePath(LivingEntity mob, int rank, long now) {
        Location loc = mob.getLocation();
        long expiry = now + (6L + 2L * rank) * 20L;
        paths.add(new Path(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), expiry));
    }

    // ---- 静的管理 ----

    private static void sweep() {
        // 期限切れの道を撤去しつつ、生存している道はバリアパーティクルで描画する。
        Iterator<Path> it = paths.iterator();
        while (it.hasNext()) {
            Path p = it.next();
            if (p.world.getGameTime() >= p.expiry) {
                it.remove();
                continue;
            }
            // BLOCK_MARKER をバリアのブロックデータで出すと、その位置に赤いバリアブロックが表示される。
            p.world.spawnParticle(Particle.BLOCK_MARKER,
                    p.x, p.y + 0.01, p.z, 1, 0, 0, 0, 0, BARRIER_DATA);
        }
        if (paths.isEmpty()) {
            lastPlace.clear(); // 道が無ければスロットル情報も不要（リーク防止）
            return;
        }
        // 道に触れているプレイヤーへ各デバフを付け直す（時限なので離れれば失効）。
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

    /**
     * 防御力・防具強度・各種ダメージ軽減を 0 に固定し、最大HP・攻撃力を本来の {@link #STAT_MULT}（80%）へ
     * 引き下げる（時限 transient、毎周期で付け直す。道から離れれば失効）。
     */
    private static void applyDecay(Player player) {
        Attributes.removeAll(player, SOURCE);
        Attributes.addTransient(player, Vanilla.ARMOR, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, Vanilla.ARMOR_TOUGHNESS, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.PHYSICAL_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.MAGIC_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.FIRE_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.LIGHTNING_RESIST, SOURCE, Operation.SET, 0.0, EFFECT_TICKS);
        Attributes.addTransient(player, StandardAttributes.DAMAGE_TAKEN, SOURCE, Operation.SET, 1.0, EFFECT_TICKS);
        // 最大HP・攻撃力を本来の 80% へ（MAX_HEALTH 減少時の現在HPクランプはバニラ仕様に従う）。
        Attributes.addTransient(player, Vanilla.MAX_HEALTH, SOURCE, Operation.MULTIPLY, STAT_MULT, EFFECT_TICKS);
        Attributes.addTransient(player, Vanilla.ATTACK_DAMAGE, SOURCE, Operation.MULTIPLY, STAT_MULT, EFFECT_TICKS);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.05, 0.2, 0.0);
    }

    private static boolean isVictim(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    /** 設置済みの凋落の道1つ。 */
    private record Path(World world, double x, double y, double z, long expiry) {
    }
}
