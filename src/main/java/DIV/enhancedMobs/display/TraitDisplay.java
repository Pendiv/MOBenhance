package DIV.enhancedMobs.display;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.config.MainConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Floating "Lv.X  TRAITS" text above a mob's head, via a {@link TextDisplay} passenger (kept
 * separate from the mob's name so kill logs stay clean).
 *
 * <p>Lifecycle: each display is tracked by mob UUID -> display UUID and tagged with a PDC marker.
 * Cleanup runs on death AND on any entity removal (despawn, /kill, void) so no tag is left floating;
 * displays are re-attached when chunks load again.
 */
public final class TraitDisplay {

    private final EnhancedMobs plugin;
    private final MainConfig config;
    private final NamespacedKey markerKey;
    private final Map<UUID, UUID> active = new HashMap<>();

    public TraitDisplay(EnhancedMobs plugin, MainConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.markerKey = new NamespacedKey(plugin, "display_marker");
    }

    public void attach(LivingEntity mob, int level, String traitText) {
        if (!config.headEnabled) {
            return;
        }
        cleanup(mob);

        World world = mob.getWorld();
        TextDisplay display = world.spawn(mob.getLocation(), TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setViewRange((float) (config.headViewDistance / 64.0));
            td.text(buildText(level, traitText));
            td.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            Transformation current = td.getTransformation();
            td.setTransformation(new Transformation(
                    new Vector3f(0f, 0.4f, 0f),
                    current.getLeftRotation(),
                    new Vector3f(1f, 1f, 1f),
                    current.getRightRotation()));
        });
        mob.addPassenger(display);
        active.put(mob.getUniqueId(), display.getUniqueId());
    }

    private Component buildText(int level, String traitText) {
        Component text = Component.text("Lv." + level, NamedTextColor.WHITE);
        if (traitText != null && !traitText.isEmpty()) {
            text = text.append(Component.text("  " + traitText, NamedTextColor.YELLOW));
        }
        return text;
    }

    /** True if this mob currently has one of our displays attached. */
    public boolean hasDisplay(LivingEntity mob) {
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay td && isOurs(td)) {
                return true;
            }
        }
        return false;
    }

    /** Remove this mob's display (tracked by UUID, plus a passenger-scan fallback). */
    public void cleanup(LivingEntity mob) {
        UUID displayId = active.remove(mob.getUniqueId());
        if (displayId != null) {
            Entity display = Bukkit.getEntity(displayId);
            if (display != null) {
                display.remove();
            }
        }
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay td && isOurs(td)) {
                td.remove();
            }
        }
    }

    /** Remove every display we created across all worlds (disable / orphan sweep). */
    public void removeAll() {
        active.clear();
        for (World w : plugin.getServer().getWorlds()) {
            for (TextDisplay td : w.getEntitiesByClass(TextDisplay.class)) {
                if (isOurs(td)) {
                    td.remove();
                }
            }
        }
    }

    public void sweepOrphans() {
        removeAll();
    }

    private boolean isOurs(TextDisplay td) {
        return td.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }
}
