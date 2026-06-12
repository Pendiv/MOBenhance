package DIV.enhancedMobs.trait.gtsolo;

import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 不思議の国のアリス症候群 — ゾンビ系（ゾンビ/ジャイアント）専用。
 * 全ダメージを35%軽減し、プレイヤーから攻撃を受けると8%で
 * {通常ゾンビ / ジャイアント / 子供ゾンビ} のプールから再抽選して変身する（同形態なら変化なし）。
 * 変身時は主要属性base・装備・PDC（レベル・特性・スタック）・体力を新個体へ引き継ぐ。
 */
public final class AliceSyndromeTrait extends Trait {

    /** 変身確率（被弾1回あたり）。 */
    private static final double TRANSFORM_CHANCE = 0.08;

    /** 変身時に base 値を引き継ぐ主要属性。 */
    private static final Attribute[] COPY_ATTRIBUTES = {
            Attribute.MAX_HEALTH, Attribute.ATTACK_DAMAGE, Attribute.MOVEMENT_SPEED,
            Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS, Attribute.KNOCKBACK_RESISTANCE
    };

    private static final EquipmentSlot[] COPY_SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public AliceSyndromeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("alice_syndrome", "ALICE", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof Zombie || mob instanceof Giant;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        // 原典どおり全ダメージ源を35%軽減（rank非依存）。attributelib の標準属性で常時適用。
        Mobs.setTraitAttribute(mob, id(), StandardAttributes.DAMAGE_TAKEN, Operation.MULTIPLY, 0.65);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player) || !appliesTo(mob)) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= TRANSFORM_CHANCE) {
            return;
        }
        // 形態プール {0: 通常ゾンビ, 1: ジャイアント, 2: 子供ゾンビ} から再抽選。同形態なら変化なし。
        int current = mob instanceof Giant ? 1 : (mob instanceof Zombie z && z.isBaby() ? 2 : 0);
        int next = random.nextInt(3);
        if (next == current) {
            return;
        }
        transform(mob, next, event);
    }

    /** 新形態の個体を生成し、属性base・装備・PDC・体力を引き継いで旧個体を消す。 */
    private void transform(LivingEntity mob, int form, EntityDamageEvent event) {
        double keepHealth = Math.max(1.0, mob.getHealth() - event.getFinalDamage());
        Class<? extends LivingEntity> shape = form == 1 ? Giant.class : Zombie.class;
        LivingEntity created = mob.getWorld().spawn(mob.getLocation(), shape, copy -> {
            // PDC を丸ごと引き継ぐ。LEVEL が立つため CreatureSpawnEvent 側の再レベリングは走らない。
            mob.getPersistentDataContainer().copyTo(copy.getPersistentDataContainer(), true);
            copyAttributes(mob, copy);
            copyEquipment(mob, copy);
            if (copy instanceof Zombie zombie) {
                if (form == 2) {
                    zombie.setBaby();
                } else {
                    zombie.setAdult();
                }
            }
        });

        event.setCancelled(true);

        // 特性の永続効果（属性修飾子・常時ポーション）は spawn コピーでは移らないため再初期化する。
        EnhancedMobs plugin = EnhancedMobs.get();
        plugin.traits().read(created).forEach((trait, rank) -> trait.initialize(created, rank));
        created.setHealth(Math.min(keepHealth, Mobs.maxHealth(created)));
        plugin.traitDisplay().attach(created, MobData.of(created).getLevel(),
                plugin.traits().displayText(plugin.traits().read(created)));

        plugin.traitDisplay().cleanup(mob);
        mob.remove();
    }

    private static void copyAttributes(LivingEntity from, LivingEntity to) {
        for (Attribute attribute : COPY_ATTRIBUTES) {
            AttributeInstance src = from.getAttribute(attribute);
            AttributeInstance dst = to.getAttribute(attribute);
            if (src != null && dst != null) {
                dst.setBaseValue(src.getBaseValue());
            }
        }
    }

    private static void copyEquipment(LivingEntity from, LivingEntity to) {
        EntityEquipment src = from.getEquipment();
        EntityEquipment dst = to.getEquipment();
        if (src == null || dst == null) {
            return;
        }
        for (EquipmentSlot slot : COPY_SLOTS) {
            dst.setItem(slot, src.getItem(slot));
        }
    }
}
