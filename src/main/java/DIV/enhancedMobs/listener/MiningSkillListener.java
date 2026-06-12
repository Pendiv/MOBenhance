package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一括破壊（ピッケルの付加スキル、CT なし）。
 * 起点ブロックと「同じ系統」のブロックを、起点からチェビシェフ距離 1/1/2/2（段階 0〜3）の
 * 範囲内で連結している分だけまとめて採掘する（上限 {@link ItemSkills#BULK_MAX_BLOCKS}）。
 * <p>
 * 対象はホワイトリスト（鉱石・花崗岩系）かつブラックリスト（石・深層岩・ネザーラック・
 * エンドストーンなどの汎用ブロック）非該当のもの。連鎖破壊は {@link Player#breakBlock(Block)}
 * 経由なのでドロップ・経験値・耐久消費・採掘XPはすべて通常採掘と同じ扱いになる。
 */
public final class MiningSkillListener implements Listener {

    /** ブラックリスト: 大量に連なる汎用ブロックは対象外。 */
    private static final Set<Material> BLACKLIST = Set.of(
            Material.STONE, Material.COBBLESTONE,
            Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.NETHERRACK, Material.END_STONE);

    /** ホワイトリスト（鉱石以外）: 花崗岩などの装飾石材。 */
    private static final Set<Material> WHITELIST_EXTRA = Set.of(
            Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.TUFF, Material.CALCITE, Material.ANCIENT_DEBRIS);

    /** breakBlock が再帰的に BlockBreakEvent を発火するための再入ガード。 */
    private boolean chaining;

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (chaining) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemSkills.hasBonus(held, ItemSkills.BONUS_BULK_BREAK) || ItemEnhancer.isBroken(held)) {
            return;
        }
        Material type = event.getBlock().getType();
        if (!isBulkTarget(type)) {
            return;
        }
        int range = ItemSkills.BULK_RANGE[ItemSkills.bonusStage(held)];
        List<Block> chain = collectChain(event.getBlock(), familyKey(type), range);
        chaining = true;
        try {
            for (Block block : chain) {
                // 途中でツールが壊れた／持ち替えた／破壊寸前で退避された場合は連鎖を打ち切る
                ItemStack current = player.getInventory().getItemInMainHand();
                if (!ItemSkills.hasBonus(current, ItemSkills.BONUS_BULK_BREAK)
                        || ItemEnhancer.isBroken(current)) {
                    break;
                }
                player.breakBlock(block);
            }
        } finally {
            chaining = false;
        }
    }

    /** ホワイトリスト（鉱石 = *_ORE と装飾石材）該当かつブラックリスト非該当。 */
    private static boolean isBulkTarget(Material type) {
        if (BLACKLIST.contains(type)) {
            return false;
        }
        return type.name().endsWith("_ORE") || WHITELIST_EXTRA.contains(type);
    }

    /**
     * 「同じ系統」の判定キー。深層岩型鉱石は地上型と同系統とみなす
     * （IRON_ORE と DEEPSLATE_IRON_ORE が同じ鉱脈として連鎖する）。
     */
    private static String familyKey(Material type) {
        String n = type.name();
        return n.startsWith("DEEPSLATE_") ? n.substring("DEEPSLATE_".length()) : n;
    }

    /**
     * 起点から 26 近傍 BFS で同系統ブロックを収集する（起点自身は通常処理で壊れるため含めない）。
     * 範囲は起点からのチェビシェフ距離で制限。上限は起点込みで {@link ItemSkills#BULK_MAX_BLOCKS}。
     */
    private static List<Block> collectChain(Block origin, String family, int range) {
        List<Block> result = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < ItemSkills.BULK_MAX_BLOCKS - 1) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (Math.abs(next.getX() - origin.getX()) > range
                                || Math.abs(next.getY() - origin.getY()) > range
                                || Math.abs(next.getZ() - origin.getZ()) > range
                                || !visited.add(next)
                                || !isBulkTarget(next.getType())
                                || !family.equals(familyKey(next.getType()))) {
                            continue;
                        }
                        result.add(next);
                        if (result.size() >= ItemSkills.BULK_MAX_BLOCKS - 1) {
                            return result;
                        }
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }
}
