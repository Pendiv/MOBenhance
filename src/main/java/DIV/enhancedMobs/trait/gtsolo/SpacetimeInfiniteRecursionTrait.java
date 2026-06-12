package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空族: 獲得時に最大HP +(30+5n)% を得て同量回復する。死亡時、この特性自体を半径16m内の
 * ランダムなMobへ移譲する（既ランクが高ければ維持）。移譲先がいなければ16m内の最寄りプレイヤーに
 * 憑依し（同HPバフ）、そのプレイヤーの死亡時に再び近隣Mobへ移譲する — 特性が宿主を渡り歩く。
 */
public final class SpacetimeInfiniteRecursionTrait extends Trait {

    /** 移譲・憑依の探索半径（原典 RADIUS）。 */
    private static final double RADIUS = 16.0;

    private static final NamespacedKey HP_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_recursion_hp");

    public SpacetimeInfiniteRecursionTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_infinite_recursion", "STRECUR", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        applyBuff(mob, rank);
    }

    /** 最大HP +(30+5n)%（MULTIPLY_BASE = ADD_SCALAR）を付与し、増えた分だけ回復する。 */
    private static void applyBuff(LivingEntity entity, int rank) {
        double before = Mobs.maxHealth(entity);
        Mobs.addModifier(entity, Attribute.MAX_HEALTH, HP_KEY,
                0.30 + 0.05 * rank, AttributeModifier.Operation.ADD_SCALAR);
        double gained = Mobs.maxHealth(entity) - before;
        if (gained > 0) {
            entity.setHealth(Math.min(Mobs.maxHealth(entity), entity.getHealth() + gained));
        }
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        int grantRank = Math.max(1, rank);
        if (transferToNearbyMob(mob, grantRank)) {
            return;
        }
        // 移譲先がいない: 最寄りプレイヤーに憑依（PDC にキャリアrank記録 + 同HPバフ）。
        Player carrier = Mobs.nearestPlayer(mob, RADIUS);
        if (carrier != null) {
            EntityState.setInt(carrier, "ir_carrier", grantRank);
            applyBuff(carrier, grantRank);
        }
    }

    /**
     * {@code MobListener.onDeath} から、プレイヤー死亡時に呼ばれる。
     * キャリアだった場合は憑依を解いて近隣Mobへ再移譲する（原典 onAnyDeath）。
     */
    public static void onCarrierDeath(Player player) {
        int rank = EntityState.getInt(player, "ir_carrier", 0);
        if (rank <= 0) {
            return;
        }
        EntityState.setInt(player, "ir_carrier", 0);
        removeHpModifier(player);
        transferToNearbyMob(player, rank);
    }

    /** 半径16m内の処理済みMobから乱択して本特性を移譲する。候補ゼロなら false。 */
    private static boolean transferToNearbyMob(LivingEntity center, int rank) {
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity e : center.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (e instanceof Mob target && !target.isDead() && MobData.of(target).isProcessed()) {
                candidates.add(target);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        LivingEntity pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        // addTrait は既ランクが指定以上なら維持する（原典の existing < rank 判定と同じ）。
        EnhancedMobs.get().traits().addTrait(pick, "spacetime_infinite_recursion", rank);
        return true;
    }

    private static void removeHpModifier(LivingEntity entity) {
        AttributeInstance inst = entity.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> HP_KEY.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }
}
