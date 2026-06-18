package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/** 魔法ダメージを無効化し、その半量を回復する。 */
public final class MagicalCreaturesTrait extends Trait {

    public MagicalCreaturesTrait(int cost, int weight, int maxRank, int minLevel) {
        super("magical_creatures", "MAGIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 魔法無効 = 魔法属性ダメージ軽減 100%（attributelib）。
        // カスタム魔法（DamageLib.magic/dela 等、DamageCause が MAGIC 以外で届くもの）も確実に無効化する。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.MAGIC_RESIST, Operation.ADD, 1.0);
    }

    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        // 原典の WITCH_RESISTANT_TO タグ相当: magic / indirect_magic / sonic_boom / thorns。
        // magic は MAGIC_RESIST でも無効化されるが、吸収回復のトリガーと sonic_boom/thorns（非魔法属性）の
        // ためにここでも処理する。回復は回復倍率（封印・呪い）を尊重する。
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SONIC_BOOM
                || cause == EntityDamageEvent.DamageCause.THORNS) {
            event.setCancelled(true);
            Mobs.heal(mob, event.getDamage() * 0.5);
        }
    }
}
