package DIV.enhancedMobs.core;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.Condition;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.api.VanillaAttributes;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import DIV.enhancedMobs.EnhancedMobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** 特性実装が共用するユーティリティ。 */
public final class Mobs {

    private Mobs() {
    }

    /**
     * 特性による自己複製（クローン・自身の召喚）を禁止するボス・ミニボス種別。
     * 特にウィザーは召喚直後の無敵中に新たな無敵ウィザーが湧き、周囲が爆発し続けるため厳禁。
     */
    private static final Set<EntityType> BOSSES = Set.of(
            EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

    public static boolean isBoss(EntityType type) {
        return BOSSES.contains(type);
    }

    /**
     * 原典の「毎tick確率p」を特性tick間隔（traits.tick-interval）1回分の判定に換算する。
     * 例: 原典が毎tick 0.5% なら chancePerTick(0.005) — 間隔20tickなら 10% で判定される。
     */
    public static boolean chancePerTick(double perTickChance) {
        int interval = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        return ThreadLocalRandom.current().nextDouble() < Math.min(1.0, perTickChance * interval);
    }

    public static double maxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : entity.getHealth();
    }

    public static double healthRatio(LivingEntity entity) {
        double max = maxHealth(entity);
        return max <= 0 ? 1 : entity.getHealth() / max;
    }

    /** バニラ属性モディファイアを冪等に適用（attributelib VanillaAttributes への委譲）。 */
    public static void addModifier(LivingEntity entity, Attribute attribute, NamespacedKey key,
                                   double amount, AttributeModifier.Operation op) {
        VanillaAttributes.set(entity, attribute, key, op, amount);
    }

    /**
     * 特性由来のカスタム属性モディファイアを冪等に付与する（REPLACE 運用）。
     * sourceId は「特性id + 属性キー」単位なので、initialize の再実行（ランク昇格・
     * チャンク再ロード）で重複しない。永続保存され、特性と同じくモブの寿命まで残る。
     */
    public static void setTraitAttribute(LivingEntity entity, String traitId, AttributeType type,
                                         Operation op, double value) {
        String source = "enhancedmobs:trait/" + traitId + "/" + type.key().getKey();
        Attributes.removeAll(entity, source);
        Attributes.add(entity, type, source, op, value);
    }

    /** 条件付き版: 条件（夜間のみ等）が成立している間だけ効く特性属性。 */
    public static void setTraitAttribute(LivingEntity entity, String traitId, AttributeType type,
                                         Operation op, double value, Condition condition) {
        String source = "enhancedmobs:trait/" + traitId + "/" + type.key().getKey();
        Attributes.removeAll(entity, source);
        Attributes.add(entity, type, source, op, value, condition);
    }

    /**
     * 回復倍率（attributelib heal_multiplier、呪い等）を尊重した直接回復。
     * EntityRegainHealthEvent を発火しない setHealth 経路の回復はこれを使うこと。
     */
    public static void heal(LivingEntity entity, double amount) {
        double healed = amount * Attributes.get(entity, StandardAttributes.HEAL_MULTIPLIER);
        if (healed > 0) {
            entity.setHealth(Math.min(maxHealth(entity), entity.getHealth() + healed));
        }
    }

    /**
     * 時限の回復倍率デバフ（呪い）。同じ sourceId は付け直し（リフレッシュ）になる。
     * 期限はワールド gameTime 基準（attributelib）なので再起動を跨いでも正しく失効する。
     */
    public static void healCurse(LivingEntity entity, String sourceId, double mult, int durationTicks) {
        Attributes.removeAll(entity, sourceId);
        Attributes.add(entity, StandardAttributes.HEAL_MULTIPLIER, sourceId,
                Operation.MULTIPLY, mult, durationTicks);
    }

    /**
     * 頭上表示などのパッセンジャー付きでも成立するテレポート。
     * 素の teleport はパッセンジャーがいると失敗する（TeleportFlag.EntityState は削除予定 API）ため、
     * いったん降ろして同行テレポートさせ、再搭乗させる。
     */
    public static boolean teleport(LivingEntity mob, Location dest) {
        List<Entity> passengers = new ArrayList<>(mob.getPassengers());
        passengers.forEach(mob::removePassenger);
        boolean ok = mob.teleport(dest);
        for (Entity passenger : passengers) {
            if (passenger.isValid()) {
                passenger.teleport(ok ? dest : mob.getLocation());
                mob.addPassenger(passenger);
            }
        }
        return ok;
    }

    public static Player nearestPlayer(LivingEntity mob, double range) {
        Player best = null;
        double bestSq = range * range;
        for (Player p : mob.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(mob.getLocation());
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    /** モブから対象に向けて矢を発射する。 */
    public static Arrow shootArrow(LivingEntity mob, LivingEntity target, double speed) {
        Location eye = mob.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (dir.lengthSquared() < 1e-6) {
            dir = mob.getLocation().getDirection();
        }
        dir.normalize().multiply(speed);
        Arrow arrow = mob.getWorld().spawn(eye, Arrow.class);
        arrow.setShooter(mob);
        arrow.setVelocity(dir);
        return arrow;
    }
}
