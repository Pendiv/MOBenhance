package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * あらゆる被ダメージの最終値を 1.0 に制限する（原典は hurt 値を 1.0 に cap）。
 * 通常の武器ダメージだけでなく、レベリングシステム由来の上昇量（武器レベル加算・
 * 武器スキルの属性ダメージ等、EntityDamageEvent として届くもの）もまとめて 1.0 に抑える。
 * 無効化不能ダメージ（/kill・奈落・ワールド境界）は {@code MobListener} が事前に除外するため届かない。
 */
public final class DivineMightTrait extends Trait {

    public DivineMightTrait(int cost, int weight, int maxRank, int minLevel) {
        super("divine_might", "DIVINE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 防具・耐性適用後の最終ダメージが 1.0 になるよう基礎値を比例縮小。
        double finalDamage = event.getFinalDamage();
        if (finalDamage > 1.0) {
            event.setDamage(event.getDamage() / finalDamage);
        }
    }
}
