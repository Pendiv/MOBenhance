package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.debug.DebugViewers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;

/**
 * Shows a mob's level and stats in the attacker's action bar when they hit it.
 * Only active for players who toggled it on via {@code /emdebug}. Development scaffolding.
 */
public final class DebugListener implements Listener {

    private static final String NS = "enhancedmobs";

    private final DebugViewers viewers;

    public DebugListener(DebugViewers viewers) {
        this.viewers = viewers;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        Player viewer = resolvePlayer(event.getDamager());
        if (viewer == null || !viewers.isViewing(viewer.getUniqueId())) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity mob)) {
            return;
        }
        viewer.sendActionBar(build(mob));
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    private Component build(LivingEntity mob) {
        int level = MobData.of(mob).getLevel();
        String text = "Lv" + level
                + "  HP " + fmt(mob.getHealth(), 1) + "/" + valStr(mob, Attribute.MAX_HEALTH, 1)
                + bonStr(mob, Attribute.MAX_HEALTH, 1)
                + "  ARM " + valStr(mob, Attribute.ARMOR, 1) + bonStr(mob, Attribute.ARMOR, 1)
                + "  ATK " + valStr(mob, Attribute.ATTACK_DAMAGE, 1) + bonStr(mob, Attribute.ATTACK_DAMAGE, 1)
                + "  SPD " + valStr(mob, Attribute.MOVEMENT_SPEED, 3) + bonStr(mob, Attribute.MOVEMENT_SPEED, 3);
        return Component.text(text, NamedTextColor.WHITE);
    }

    private String valStr(LivingEntity mob, Attribute attribute, int decimals) {
        AttributeInstance inst = mob.getAttribute(attribute);
        return inst == null ? "—" : fmt(inst.getValue(), decimals);
    }

    /** Our enhancement contribution to an attribute (sum of modifiers in our namespace). */
    private String bonStr(LivingEntity mob, Attribute attribute, int decimals) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return "";
        }
        double sum = 0;
        for (AttributeModifier modifier : inst.getModifiers()) {
            if (NS.equals(modifier.getKey().getNamespace())) {
                sum += modifier.getAmount();
            }
        }
        return sum == 0 ? "" : " (+" + fmt(sum, decimals) + ")";
    }

    private String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
