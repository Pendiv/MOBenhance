package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 攻撃が炎属性に変換される（原典 SkyScorchingFlame / Phase15Handler）。
 *
 * <p>命中時に 160 tick 着火し、対象がプレイヤーなら「焼印」を刻む。焼印中のプレイヤーは
 * <b>自然には鎮火しない</b>（10 tick ごとに炎上時間を 60 tick まで再補充。水等で消火されたら
 * 焼印解除）。さらに焼印中に炎上ダメージを受けるたび、現在体力の (7+3n)% が追加される
 * （追加分は {@link DIV.enhancedMobs.listener.PlayerListener} から
 * {@link #amplifyFireDamage} 経由で適用）。
 */
public final class SkyScorchingFlameTrait extends Trait {

    private static final String MARK_KEY = "sky_flame";
    private static final int FIRE_TICKS = 160; // 原典: setSecondsOnFire(8) 固定
    private static final int REFILL_THRESHOLD = 60;
    private static final int CHECK_INTERVAL = 10; // 原典: 10 tick ごとの PlayerTick

    public SkyScorchingFlameTrait(int cost, int weight, int maxRank, int minLevel) {
        super("sky_scorching_flame", "SCORCH", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        target.setFireTicks(Math.max(target.getFireTicks(), FIRE_TICKS));
        if (target instanceof Player player) {
            EntityState.setInt(player, MARK_KEY, rank); // 焼印 = 特性ランク
            startMarkTask(player);
        }
    }

    /**
     * 焼印中プレイヤーの炎上ダメージ（FIRE / FIRE_TICK）に現在体力 × (0.07 + 0.03n) を加算する。
     * プレイヤー被弾は MobListener の特性ディスパッチ対象外のため PlayerListener から呼ばれる。
     */
    public static void amplifyFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) {
            return;
        }
        int rank = EntityState.getInt(player, MARK_KEY, 0);
        if (rank > 0) {
            event.setDamage(event.getDamage() + player.getHealth() * (0.07 + 0.03 * rank));
        }
    }

    /** 再ログイン時の焼印再開（燃えたまま再参加した場合のみ。鎮火済みなら焼印を掃除）。 */
    public static void resumeMark(Player player) {
        if (EntityState.getInt(player, MARK_KEY, 0) <= 0) {
            return;
        }
        if (player.getFireTicks() > 0) {
            startMarkTask(player);
        } else {
            EntityState.setInt(player, MARK_KEY, 0);
        }
    }

    /** 焼印の周期処理: 10 tick ごとに鎮火阻止（60 tick 補充）と消火検知（焼印解除）を行う。 */
    private static void startMarkTask(Player player) {
        if (FastTick.isRegistered(player, MARK_KEY)) {
            return;
        }
        int[] countdown = {CHECK_INTERVAL};
        FastTick.register(player, MARK_KEY, () -> {
            if (!player.isValid()) {
                return false; // ログアウト等 → 再ログイン時に resumeMark が再開
            }
            if (--countdown[0] > 0) {
                return true;
            }
            countdown[0] = CHECK_INTERVAL;
            if (EntityState.getInt(player, MARK_KEY, 0) <= 0) {
                return false;
            }
            if (player.getFireTicks() <= 0) {
                EntityState.setInt(player, MARK_KEY, 0); // 水等で消火 → 焼印解除
                return false;
            }
            if (player.getFireTicks() < REFILL_THRESHOLD) {
                player.setFireTicks(REFILL_THRESHOLD); // 自然鎮火しない
            }
            return true;
        });
    }
}
