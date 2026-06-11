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
 * モブ頭上に浮かぶ「Lv.X  特性名」テキスト。{@link TextDisplay} をパッセンジャーとして使用し、
 * キルログをモブ名と分離する。
 *
 * <p>ライフサイクル: モブ UUID → Display UUID で追跡し PDC マーカーで識別。
 * 死亡・除去（デスポーン・/kill・奈落）いずれでもクリーンアップされるため残留しない。
 * チャンクリロード時に再アタッチされる。
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

    /** このモブに当プラグインの Display がアタッチされているか。 */
    public boolean hasDisplay(LivingEntity mob) {
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay td && isOurs(td)) {
                return true;
            }
        }
        return false;
    }

    /** このモブの Display を除去（UUID 追跡 + パッセンジャースキャンの二重確認）。 */
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

    /** 全ワールドの当プラグイン製 Display をすべて除去（無効化・孤立スイープ用）。 */
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
