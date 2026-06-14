package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

/**
 * 強化システム対象のツール・防具を破壊から保護する。
 * 残耐久が 10 以下になった時点で「壊れた」扱いとなり、装備スロット（ホットバー・オフハンド・防具）
 * から本体インベントリ（上段 9〜35）へ自動退避する。空きが無い場合はメッセージのみ。
 * 閾値以降の耐久ダメージは常にキャンセルされるため、アイテムが消滅することはない。
 */
public final class ItemBreakGuardListener implements Listener {

    /** この残耐久以下で「壊れた」扱い。 */
    private static final int BROKEN_THRESHOLD = 10;

    /** 本体インベントリ（ホットバーを除く上段）のスロット範囲。 */
    private static final int STORAGE_FIRST = 9;
    private static final int STORAGE_LAST = 35;

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!ItemEnhancer.isEnhanceable(item) || !(item.getItemMeta() instanceof Damageable damageable)) {
            return;
        }
        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        int remaining = max - (damageable.getDamage() + event.getDamage());
        if (remaining > BROKEN_THRESHOLD) {
            return;
        }
        // 閾値到達: この耐久ダメージは入れず、破壊寸前状態（性能0）にして装備スロットから退避する
        event.setCancelled(true);
        ItemEnhancer.markBroken(item);
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        int target = firstFreeStorageSlot(inv);
        if (target == -1) {
            Lang.actionbar(player, "emob.break.no_space");
            return;
        }
        int source = findSlot(inv, item);
        if (source == -1 || source == target) {
            return;
        }
        inv.setItem(target, item);
        inv.setItem(source, null);
        Lang.actionbar(player, "emob.break.stowed");
    }

    /**
     * 破壊寸前の自動撤回スイープ（A案）。{@link ItemEnhancer#clearBrokenIfRepaired} を呼ぶのは
     * 独自金床修理とメンディングの2経路だけなので、それ以外の耐久回復（本物の金床GUI・砥石・
     * 作業台合成・コマンド等）では破壊寸前フラグが残り続けてしまう。これを補うため、低頻度で
     * オンラインプレイヤーの所持品を走査し、耐久が閾値（75%）以上まで戻っている破壊寸前の
     * ツール/防具のフラグを解除する。
     */
    public static void startBrokenSweep(EnhancedMobs plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.isEmpty()) {
                        continue;
                    }
                    if (ItemEnhancer.isBroken(item) && ItemEnhancer.clearBrokenIfRepaired(item)) {
                        Lang.actionbar(player, "emob.break.cleared");
                    }
                }
            }
        }, 200L, 200L); // 10秒周期
    }

    /** メンディング等の経験値修理でも、75% 以上回復したら破壊寸前を自動解除する。 */
    @EventHandler(ignoreCancelled = true)
    public void onMend(PlayerItemMendEvent event) {
        ItemStack item = event.getItem();
        if (!ItemEnhancer.isBroken(item)) {
            return;
        }
        // 修理量はイベント後に適用されるため、1tick 後の実耐久で判定する
        Bukkit.getScheduler().runTask(EnhancedMobs.get(), () -> ItemEnhancer.clearBrokenIfRepaired(item));
    }

    /** 上段インベントリ（9〜35）の最初の空きスロット。無ければ -1。 */
    private static int firstFreeStorageSlot(PlayerInventory inv) {
        for (int i = STORAGE_FIRST; i <= STORAGE_LAST; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** ダメージを受けたアイテムの実スロット（ホットバー・本体・防具 36〜39・オフハンド 40）を同一参照で特定。 */
    private static int findSlot(PlayerInventory inv, ItemStack item) {
        for (int i = 0; i <= 40; i++) {
            if (inv.getItem(i) == item) {
                return i;
            }
        }
        // 参照が一致しない実装系へのフォールバック（equals 比較）
        for (int i = 0; i <= 40; i++) {
            if (item.equals(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
