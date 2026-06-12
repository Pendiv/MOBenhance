package DIV.enhancedMobs.core;

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

    /** アトリビュートモディファイアを冪等に適用（既存を削除してから追加）。 */
    public static void addModifier(LivingEntity entity, Attribute attribute, NamespacedKey key,
                                   double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
        inst.addModifier(new AttributeModifier(key, amount, op));
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
