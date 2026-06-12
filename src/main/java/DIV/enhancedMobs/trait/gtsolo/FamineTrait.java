package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 飢餓 — CT (36-3×rank) 秒ごとに、半径16のサバイバル/アドベンチャーのプレイヤーへ
 * 空腹 I を (10+2×rank) 秒付与する波状デバフ。さらに 32m 圏内のプレイヤーに
 * {@code famine_zone} フラグを付け、空腹エフェクト中の食事の満腹度回復を
 * {@code PlayerListener} 側で丸ごと巻き戻す（飢餓持ちの前では食事が無意味）。
 */
public final class FamineTrait extends Trait {

    private static final double EFFECT_RADIUS = 16.0;
    private static final double MEAL_BLOCK_RADIUS = 32.0;

    public FamineTrait(int cost, int weight, int maxRank, int minLevel) {
        super("famine", "FAMINE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 食事巻き戻し判定用の圏内フラグ（次tickまで+余裕分）。
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        for (Entity entity : mob.getNearbyEntities(MEAL_BLOCK_RADIUS, MEAL_BLOCK_RADIUS, MEAL_BLOCK_RADIUS)) {
            if (entity instanceof Player player) {
                EntityState.setFlag(player, "famine_zone", interval + 10);
            }
        }

        // CT (36-3×rank) 秒の波状付与（下限1秒）。
        if (EntityState.hasFlag(mob, "famine_cd")) {
            return;
        }
        EntityState.setFlag(mob, "famine_cd", Math.max(20, (36 - 3 * rank) * 20));
        int duration = (10 + 2 * rank) * 20;
        for (Entity entity : mob.getNearbyEntities(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)) {
            if (entity instanceof Player player
                    && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0, true, true, true));
            }
        }
    }

    /**
     * 飢餓の食事巻き戻し（PlayerListener から呼ばれる）。空腹エフェクト中に飢餓持ちの
     * 圏内で食事しても、満腹度・隠し満腹度の回復を1tick後に丸ごと巻き戻す
     * （イベントはキャンセルしない = アイテムは消費される）。
     */
    public static void rollbackMeal(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPotionEffect(PotionEffectType.HUNGER) || !EntityState.hasFlag(player, "famine_zone")) {
            return;
        }
        int foodLevel = player.getFoodLevel();
        float saturation = player.getSaturation();
        EnhancedMobs plugin = EnhancedMobs.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.setFoodLevel(foodLevel);
                player.setSaturation(saturation);
            }
        });
    }
}
