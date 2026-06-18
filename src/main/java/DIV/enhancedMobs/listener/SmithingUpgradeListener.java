package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 鍛冶台でのマテリアル変更（ダイヤ→ネザライト等）を決定論化する。
 *
 * <p>バニラのコンポーネントコピー任せだと「強化データが運ばれるか不確定」「運ばれても
 * ステータスがダイヤ基礎のまま次の rebuild まで固まる」という問題がある。ここでは強化済み
 * アイテムのアップグレード結果を、<b>ベースの全メタ（PDC/エンチャ/名前/耐久）を新マテリアルへ
 * 移し替え → 総経験値からレベルを再導出 → {@link ItemEnhancer#refresh} で再構築</b>した
 * 決定論的な結果に差し替える。進行（スキル・精錬等）は保持しつつ、上限（70→100）・精錬上限
 * （5→10）・基礎ステータスが即追従する。</p>
 *
 * <p><b>レベルは維持しない</b>: ダイヤとネザライトでは必要XPカーブが異なるため、レベル番号を
 * そのまま引き継ぐと「重いカーブのぶんを踏み倒す」ことになる。代わりに<b>総経験値</b>を維持し、
 * 新素材のカーブで等価なレベルへ再導出する（同じ投入XPでもネザライトでは相応にレベルが下がる）。</p>
 *
 * <p>マテリアル不変（スミシングトリム等）は対象外（バニラが PDC を保持するため触らない）。</p>
 */
public final class SmithingUpgradeListener implements Listener {

    @EventHandler
    public void onPrepare(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) {
            return; // 有効なレシピが成立していない
        }
        ItemStack base = event.getInventory().getInputEquipment();
        if (!ItemEnhancer.isEnhanced(base)) {
            return; // 強化済みアイテムのアップグレードのみ決定論化
        }
        if (result.getType() == base.getType()) {
            return; // マテリアル不変（トリム付与など）は触らない
        }
        // 旧素材のカーブで総投入経験値を確定（ベースはまだダイヤ等）。
        int totalXp = ItemEnhancer.totalInvestedXp(base);
        // ベースの全メタを新マテリアルへ移植。
        ItemStack upgraded = new ItemStack(result.getType());
        upgraded.setItemMeta(base.getItemMeta());
        // レベルは維持せず、総経験値から新素材のカーブで再導出（レベル踏み倒し防止）。
        ItemEnhancer.setLevelFromTotalXp(upgraded, totalXp);
        // 新素材・新レベル基準でステータス・耐久・上限・ロアを再構築。
        ItemEnhancer.refresh(upgraded);
        event.setResult(upgraded);
    }
}
