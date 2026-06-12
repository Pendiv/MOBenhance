package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * プレイヤーへの与ダメージ時、確率 (12+3N)% で発動: プレイヤーに (15+15N) 秒の強デバフ
 * （原典の上級スキルノード無効化の近似: 弱体化II + 採掘疲労II）を与える代わりに、
 * 自身も同時間の封印状態（最大HP −(55−N)%・防具/防具強度 0）に入る。封印中は再発動しない。
 */
public final class DesperateChargeTrait extends Trait {

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
        // プレイヤー側: スキルノード無効化の最良近似（能力を1つ失った感のある強デバフ）+ 通知
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 1, true, true, true));
        player.sendActionBar(Component.text("決死の特攻！ " + seconds + "秒間、力を封じられた", NamedTextColor.RED));
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

    private static NamespacedKey key(String name) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_" + name);
    }
}
