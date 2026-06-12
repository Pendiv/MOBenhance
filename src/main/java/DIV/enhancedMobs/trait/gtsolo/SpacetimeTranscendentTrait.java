package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 半径12mの「時空タイプを持たないMob」を即消滅させる（死亡イベント・ドロップなし）。
 * ただし15%の確率で殺さず、時空プール13種から抽選した特性を rank=max(1, level) で付与して同類化する。
 * 自身は他Mobの AIターゲットから除外される（原典 TargetingConditionsMixin 相当）。
 */
public final class SpacetimeTranscendentTrait extends AuraTrait {

    /** 殺さずに同類化する確率（原典 CONVERT_CHANCE）。 */
    private static final double CONVERT_CHANCE = 0.15;

    public SpacetimeTranscendentTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_transcendent", "STTRAN", cost, weight, maxRank, minLevel, 12.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (!(target instanceof Mob victim) || MobTags.has(victim, "spacetime")) {
            return;
        }
        // 15%: 見逃して同類化（特性を持てる処理済み個体のみ。原典の isProper 相当）。
        if (ThreadLocalRandom.current().nextDouble() < CONVERT_CHANCE && MobData.of(victim).isProcessed()) {
            String granted = SpacetimeTraits.randomGrantableId();
            boolean holds = EnhancedMobs.get().traits().read(victim).keySet().stream()
                    .anyMatch(t -> t.id().equals(granted));
            if (!holds) {
                // 既所持ならスキップ（原典どおり）。タグ・頭上表示は addTrait → initialize が付ける。
                EnhancedMobs.get().traits().addTrait(victim, granted, Math.max(1, rank));
            }
        } else if (!Mobs.isBoss(victim.getType())) {
            // 即消滅（原典 discard 相当 — 死亡イベントなし＝ドロップ・経験値なし）。
            // ボスは戦闘進行が壊れるためプラグイン側の安全装置として対象外。
            victim.remove();
        }
    }

    /** AIターゲット除外: 他Mobはこのモブを狙えない（プレイヤーの攻撃には影響しない）。 */
    @Override
    public void onTargeted(LivingEntity mob, int rank, EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Mob) {
            event.setCancelled(true);
        }
    }
}
