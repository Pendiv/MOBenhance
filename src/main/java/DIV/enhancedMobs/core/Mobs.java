package DIV.enhancedMobs.core;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

/** 特性実装が共用するユーティリティ。 */
public final class Mobs {

    private Mobs() {
    }

    /** 特性による自己複製（クローン・自身の召喚）を禁止するボス・ミニボス種別。 */
    private static final Set<EntityType> BOSSES = Set.of(
            EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);
//witherは一番ダメ。償還後無敵の最中に新しい無敵witherが出現し、戦闘とか関係なく周囲が爆発し続ける
    public static boolean isBoss(EntityType type) {
        return BOSSES.contains(type);
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
