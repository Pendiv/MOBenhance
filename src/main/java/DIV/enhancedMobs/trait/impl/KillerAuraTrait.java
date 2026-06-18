package DIV.enhancedMobs.trait.impl;

import DIV.attributelib.api.DamageLib;
import DIV.enhancedMobs.core.TraitCooldown;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** 定期的に周囲のプレイヤーに魔法系ダメージを与えるオーラ。 */
public final class KillerAuraTrait extends Trait {

    private final double range;
    private final double damagePerRank;

    public KillerAuraTrait(int cost, int weight, int maxRank, int minLevel, double range, double damagePerRank) {
        super("killer_aura", "AURA", cost, weight, maxRank, minLevel);
        this.range = range;
        this.damagePerRank = damagePerRank;
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double damage = damagePerRank * rank;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player player && TraitCooldown.ready(player, "killer_aura")) {
                DamageLib.magic(mob, player, damage); // 魔法ダメージ（魔法耐性・与ダメ倍率が乗る）
            }
        }
    }
}
