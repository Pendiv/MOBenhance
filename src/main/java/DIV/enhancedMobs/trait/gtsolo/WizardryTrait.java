package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 低確率で最寄りプレイヤーと座標を入れ替える（原典 Wizardry）。
 *
 * <p>毎 tick 確率 rank/180 で起動判定（tick間隔換算）し、以下を全て通過した時のみスワップ:
 * <ol>
 *   <li>双方の足元が固体ブロック（落下中のプレイヤーを空中に飛ばさない）</li>
 *   <li>水平距離 ≤ 15 かつ 高低差 |Δy| ≤ 3</li>
 *   <li>成功率 60+10n%（上限100%）</li>
 *   <li>プレイヤー側 CT が許可（本CT中は免疫）</li>
 * </ol>
 *
 * <p>CT は原典 WizardryCooldown 準拠: 本CT 100t × 1.3^段。本CT 後に同長の予備CTがあり、
 * 予備CT中に再被弾すると段階+1（最大5）、予備CTを無被弾で過ぎると段階リセット。
 */
public final class WizardryTrait extends Trait {

    private static final double SEARCH_RADIUS = 16.0;
    private static final double MAX_HORIZONTAL_SQ = 15.0 * 15.0;
    private static final double MAX_VERTICAL = 3.0;
    private static final int BASE_CT = 100;
    private static final int MAX_STACKS = 5;
    private static final double CT_STEP = 1.3;

    public WizardryTrait(int cost, int weight, int maxRank, int minLevel) {
        super("wizardry", "WIZARD", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 原典: 毎tick確率 rank/180（毎秒換算 lv1≈10.5%）→ tick間隔換算で判定。
        if (!Mobs.chancePerTick(rank / 180.0)) {
            return;
        }
        Player player = Mobs.nearestPlayer(mob, SEARCH_RADIUS);
        if (player == null || !isFeetSolid(mob) || !isFeetSolid(player)) {
            return;
        }
        // 対象範囲: 水平15以内 かつ 高低差±3以内でのみ発生。
        double dx = player.getLocation().getX() - mob.getLocation().getX();
        double dz = player.getLocation().getZ() - mob.getLocation().getZ();
        if (dx * dx + dz * dz > MAX_HORIZONTAL_SQ
                || Math.abs(player.getLocation().getY() - mob.getLocation().getY()) > MAX_VERTICAL) {
            return;
        }
        // 成功率 60+10n%（上限100%）。
        if (ThreadLocalRandom.current().nextInt(100) >= Math.min(100, 60 + 10 * rank)) {
            return;
        }
        if (!tryHit(player)) {
            return;
        }
        Location mobLoc = mob.getLocation();
        Mobs.teleport(mob, player.getLocation());
        player.teleport(mobLoc);
    }

    /** 落下中のプレイヤーを空中へ飛ばさないよう、足元が固体ブロックの時のみ発火する。 */
    private static boolean isFeetSolid(LivingEntity entity) {
        return entity.getLocation().subtract(0, 1, 0).getBlock().getType().isSolid();
    }

    /**
     * プレイヤー側 CT（原典 WizardryCooldown 準拠の指数モデル）。
     * 本CT中は false（免疫）。予備CT中の被弾は次CTを ×1.3 に延長、無被弾経過で初期化。
     *
     * @return 発動可なら true（同時に CT を更新する）
     */
    private static boolean tryHit(Player player) {
        int now = Bukkit.getCurrentTick();
        if (now < EntityState.getInt(player, "wiz_ct_end", 0)) {
            return false; // 本CT中 → 免疫
        }
        int stacks = now < EntityState.getInt(player, "wiz_res_end", 0)
                ? Math.min(MAX_STACKS, EntityState.getInt(player, "wiz_stacks", 0) + 1) // 予備CT中被弾 → 段階up
                : 0; // 予備CTも無被弾で経過 → 初期化
        int ct = (int) Math.round(BASE_CT * Math.pow(CT_STEP, stacks));
        EntityState.setInt(player, "wiz_stacks", stacks);
        EntityState.setInt(player, "wiz_ct_end", now + ct);
        EntityState.setInt(player, "wiz_res_end", now + ct * 2); // 予備CTは本CTと同長
        return true;
    }
}
