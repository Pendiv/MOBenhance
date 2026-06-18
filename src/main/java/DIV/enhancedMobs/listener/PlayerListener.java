package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.PlayerData;
import DIV.enhancedMobs.trait.gtsolo.FamineTrait;
import DIV.enhancedMobs.trait.gtsolo.MediatorFieldTrait;
import DIV.enhancedMobs.trait.gtsolo.SkyScorchingFlameTrait;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/** 各プレイヤーが入った特殊ディメンションを記録する（難易度要素3）。 */
public final class PlayerListener implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        mark(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // ネザー/エンドに直接ログインしたプレイヤーを捕捉する。
        mark(event.getPlayer());
        // 天を焼く焔: 燃えたまま再ログインしたプレイヤーの焼印を再開する。
        SkyScorchingFlameTrait.resumeMark(event.getPlayer());
        // 追加レシピをレシピブックに解禁する。
        event.getPlayer().discoverRecipe(EnhancedMobs.elytraRecipeKey());
    }

    /** 天を焼く焔: 焼印中プレイヤーの炎上ダメージに現在体力比例の追撃を加算する。 */
    @EventHandler
    public void onFireDamage(EntityDamageEvent event) {
        SkyScorchingFlameTrait.amplifyFireDamage(event);
    }

    /** 事故救済: 同一MC日に死亡が嵩むほど、そのプレイヤーが受ける危険度（モブレベル）を下げる。 */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        long day = Bukkit.getWorlds().get(0).getGameTime() / 24000L;
        PlayerData.of(event.getEntity()).recordDeathRelief(day);
    }

    /** 飢餓: 空腹エフェクト中に飢餓持ち（32m圏内）の前で食事しても満腹度回復を巻き戻す。 */
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        FamineTrait.rollbackMeal(event);
    }

    /** 媒介野: 罠ブロックを踏んだプレイヤーへデバフを付与する。 */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        MediatorFieldTrait.onPlayerMove(event);
    }

    /** 媒介野: 罠ブロックは壊すと消えるのではなく元のブロックへ復元される。 */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        MediatorFieldTrait.onBlockBreak(event);
    }

    /** 媒介野: チャンクアンロード時に域内の罠を一括復元する（永続グリーフィング防止）。 */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        MediatorFieldTrait.onChunkUnload(event);
    }

    private void mark(Player player) {
        World.Environment env = player.getWorld().getEnvironment();
        PlayerData data = PlayerData.of(player);
        if (env == World.Environment.NETHER) {
            data.setVisitedNether();
        } else if (env == World.Environment.THE_END) {
            data.setVisitedEnd();
        }
    }
}
