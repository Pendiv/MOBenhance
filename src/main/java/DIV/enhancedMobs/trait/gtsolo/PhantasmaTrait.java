package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * プレイヤーから3回被弾するか、24/(rank+1) 秒のクールタイムで「幻惑」を1層獲得（最大 {@link #MAX_LAYERS} 層）。
 * 幻惑は1層につきプレイヤー被ダメを {@link #DR_PER_LAYER}（10%）軽減する。
 * プレイヤーから攻撃を受けるたびに（保持層数ぶん軽減したうえで）幻惑が1層消滅する。
 */
public final class PhantasmaTrait extends Trait {

    /** 幻惑の保持層数。 */
    private static final String STACKS = "phantasma_illusion";
    private static final String HITS = "phantasma_hits";
    private static final String CD = "phantasma_cd";

    /** 1層あたりの軽減率。 */
    private static final double DR_PER_LAYER = 0.10;
    /** 幻惑の最大層数（= 最大軽減 50%）。 */
    private static final int MAX_LAYERS = 5;

    public PhantasmaTrait(int cost, int weight, int maxRank, int minLevel) {
        super("phantasma", "PHANTASMA", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, STACKS, 0) >= MAX_LAYERS) {
            return; // 上限到達中はCDを進めない
        }
        int now = (int) EntityState.gameTime();
        double cd = EntityState.getDouble(mob, CD, -1);
        if (cd < 0) {
            EntityState.setDouble(mob, CD, now); // 初回はCD起点だけ記録
            return;
        }
        if (now - cd >= 24.0 / (rank + 1) * 20) {
            gainLayer(mob, now);
        }
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player)) {
            return; // プレイヤー由来のみカウント・軽減
        }
        int now = (int) EntityState.gameTime();
        int stacks = EntityState.getInt(mob, STACKS, 0);
        if (stacks > 0) {
            // 保持層数ぶん軽減し、1層消費する。
            double reduction = Math.min(0.9, DR_PER_LAYER * stacks);
            event.setDamage(event.getDamage() * (1.0 - reduction));
            EntityState.setInt(mob, STACKS, stacks - 1);
            EntityState.setDouble(mob, CD, now); // 消費後はCD再起点
            return;
        }
        if (EntityState.addInt(mob, HITS, 1) >= 3) {
            gainLayer(mob, now);
        }
    }

    private static void gainLayer(LivingEntity mob, int now) {
        EntityState.setInt(mob, STACKS, Math.min(MAX_LAYERS, EntityState.getInt(mob, STACKS, 0) + 1));
        EntityState.setInt(mob, HITS, 0);
        EntityState.setDouble(mob, CD, now);
    }
}
