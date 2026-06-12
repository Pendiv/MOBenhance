package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/** 近くのプレイヤーの浮遊・低速落下・飛行・エリトラ滑空を5tickおきに解除する。 */
public final class GroundBattleTrait extends Trait {

    private static final double RADIUS = 32.0;
    private static final int PERIOD = 5;

    public GroundBattleTrait(int cost, int weight, int maxRank, int minLevel) {
        super("ground_battle", "GROUND", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        ensureRegistered(mob);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // チャンク再ロード等で FastTick 登録が消えた場合の再登録
        ensureRegistered(mob);
    }

    private static void ensureRegistered(LivingEntity mob) {
        if (FastTick.isRegistered(mob, "ground_battle")) {
            return;
        }
        FastTick.register(mob, "ground_battle", new FastTick.Handler() {
            private int count;

            @Override
            public boolean tick() {
                if (!mob.isValid()) {
                    return false;
                }
                if (++count % PERIOD == 0) {
                    sweep(mob);
                }
                return true;
            }
        });
    }

    /** 半径32の生存プレイヤー（クリエイティブ・スペクテイター除外）を地面に縛り付ける。 */
    private static void sweep(LivingEntity mob) {
        for (Entity entity : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                continue;
            }
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            if (player.isFlying()) {
                player.setFlying(false);
            }
            if (player.isGliding()) {
                player.setGliding(false);
            }
        }
    }
}
