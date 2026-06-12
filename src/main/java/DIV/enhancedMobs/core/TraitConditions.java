package DIV.enhancedMobs.core;

import DIV.attributelib.api.Condition;
import DIV.attributelib.api.Conditions;
import DIV.enhancedMobs.EnhancedMobs;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

/**
 * EnhancedMobs が登録するカスタム適用条件（attributelib Conditions）。
 * attributelib 標準の DAY/NIGHT は {@code World.isDayTime()} 基準で、
 * L2 原典の昼夜窓（夜 = dayTime%24000 ∈ [13000,23000)）とズレるため、原典窓を自前登録する。
 */
public final class TraitConditions {

    /** 原典の夜窓: [13000, 23000)。夜行性（nocturnal）用。 */
    public static Condition NIGHT_WINDOW;
    /** 原典の昼窓: [0,13000) ∪ [23000,24000)。昼行性（diurnal）用。 */
    public static Condition DAY_WINDOW;

    private TraitConditions() {
    }

    /** onEnable で1回だけ呼ぶ（特性の initialize より先）。 */
    public static void init(EnhancedMobs plugin) {
        NIGHT_WINDOW = Conditions.register(plugin, "night_window", Component.text("夜間"),
                entity -> isNightWindow(entity));
        DAY_WINDOW = Conditions.register(plugin, "day_window", Component.text("昼間"),
                entity -> !isNightWindow(entity));
    }

    private static boolean isNightWindow(LivingEntity entity) {
        long t = entity.getWorld().getTime() % 24000;
        return t >= 13000 && t < 23000;
    }
}
