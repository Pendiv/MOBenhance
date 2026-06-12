package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 周囲（半径12）の敵味方を問わない全 LivingEntity（プレイヤー含む）の攻撃力を
 * -(30+10n)% 低下させるデバフフィールド（原典 Laziness）。
 *
 * <p>modifier は transient（NBT 非永続）で付与するため、サーバ再起動で確実に消える
 * （= 原典の永続化バグ対策と同等の安全性）。範囲外へ出た対象は保持者ごとの
 * 追跡セットで検知して即座に除去する（原典と同方式）。
 */
public final class LazinessTrait extends Trait {

    private static final double RADIUS = 12.0;

    /** 保持モブ UUID → 現在デバフ中の対象 UUID 集合（範囲外退出検知用、サーバ起動中のみ）。 */
    private static final Map<UUID, Set<UUID>> AFFECTED = new HashMap<>();

    public LazinessTrait(int cost, int weight, int maxRank, int minLevel) {
        super("laziness", "LAZY", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        double drop = -(0.30 + 0.10 * rank); // 原典: -(30 + 10n)% MULTIPLY_TOTAL
        Set<UUID> current = new HashSet<>();
        for (Entity entity : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof LivingEntity target) {
                current.add(target.getUniqueId());
                applyTransient(target, drop);
            }
        }
        // 範囲外へ出た前回対象から modifier を除去（= デバフを残さない）。
        Set<UUID> prev = AFFECTED.put(mob.getUniqueId(), current);
        if (prev != null) {
            prev.removeAll(current);
            prev.forEach(LazinessTrait::removeFrom);
        }
    }

    /** 保持者死亡時は追跡中の全対象からデバフを除去する（原典は tick 停止で残留するが安全側に倒す）。 */
    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        Set<UUID> tracked = AFFECTED.remove(mob.getUniqueId());
        if (tracked != null) {
            tracked.forEach(LazinessTrait::removeFrom);
        }
    }

    private static void applyTransient(LivingEntity target, double drop) {
        AttributeInstance inst = target.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        NamespacedKey key = key();
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
        inst.addTransientModifier(new AttributeModifier(key, drop, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private static void removeFrom(UUID targetId) {
        if (Bukkit.getEntity(targetId) instanceof LivingEntity target) {
            AttributeInstance inst = target.getAttribute(Attribute.ATTACK_DAMAGE);
            if (inst != null) {
                NamespacedKey key = key();
                inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList()
                        .forEach(inst::removeModifier);
            }
        }
    }

    private static NamespacedKey key() {
        return new NamespacedKey(EnhancedMobs.get(), "trait_laziness_atk");
    }
}
