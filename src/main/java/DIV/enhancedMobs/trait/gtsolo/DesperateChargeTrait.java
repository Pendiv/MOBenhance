package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.item.ItemSkills;
import DIV.enhancedMobs.trait.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * プレイヤーへの与ダメージ時、確率 (12+3N)% で発動: プレイヤーの武器（手持ち）レベリングスキルを
 * (15+15N) 秒間封印する（自身が不死系特性を持つ場合は防具スキルも追加で封印）代わりに、
 * 自身も同時間の封印状態（最大HP −(55−N)%・防具/防具強度 0）に入る。封印中は再発動しない。
 */
public final class DesperateChargeTrait extends Trait {

    /** 「不死系」と見なす特性 id（保持していると防具スキルまで封印される）。 */
    private static final Set<String> UNDYING_TRAITS = Set.of(
            "undying", "incomplete_combustion", "endless_tale", "second_sleep",
            "dream_melt", "second_chance", "rebirth", "trinity_life");

    public DesperateChargeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("desperate_charge", "DESPER", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player)
                || EntityState.hasFlag(mob, "dc_seal")
                || ThreadLocalRandom.current().nextDouble() >= 0.12 + 0.03 * rank) {
            return;
        }
        int seconds = 15 + 15 * rank;
        int duration = seconds * 20;
        // プレイヤー側: 武器スキルを封印（原典の上級スキルノード無効化に対応）。
        ItemSkills.lockWeaponSkills(player, duration);
        // 自身が不死系特性を持つなら、防具スキルも併せて封印する。
        boolean immortal = hasUndyingTrait(mob);
        if (immortal) {
            ItemSkills.lockArmorSkills(player, duration);
        }
        player.sendActionBar(Component.text("決死の特攻！ " + seconds + "秒間、"
                + (immortal ? "武器・防具スキルを封じられた" : "武器スキルを封じられた"), NamedTextColor.RED));
        // 自分側: 封印（原典 MULTIPLY_TOTAL = Bukkit の MULTIPLY_SCALAR_1）
        Mobs.addModifier(mob, Attribute.MAX_HEALTH, key("dc_hp"), -(0.55 - 0.01 * rank),
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ARMOR, key("dc_armor"), -1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Mobs.addModifier(mob, Attribute.ARMOR_TOUGHNESS, key("dc_tough"), -1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        double max = Mobs.maxHealth(mob);
        if (mob.getHealth() > max) {
            mob.setHealth(max);
        }
        EntityState.setFlag(mob, "dc_seal", duration);
        EntityState.setInt(mob, "dc_sealed", 1);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // 封印明けの検知（原典は 1 秒粒度の期限チェック）
        if (EntityState.getInt(mob, "dc_sealed", 0) == 1 && !EntityState.hasFlag(mob, "dc_seal")) {
            EntityState.setInt(mob, "dc_sealed", 0);
            removeModifier(mob, Attribute.MAX_HEALTH, key("dc_hp"));
            removeModifier(mob, Attribute.ARMOR, key("dc_armor"));
            removeModifier(mob, Attribute.ARMOR_TOUGHNESS, key("dc_tough"));
        }
    }

    private static void removeModifier(LivingEntity mob, Attribute attribute, NamespacedKey key) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }

    private static boolean hasUndyingTrait(LivingEntity mob) {
        return EnhancedMobs.get().traits().read(mob).keySet().stream()
                .anyMatch(t -> UNDYING_TRAITS.contains(t.id()));
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
