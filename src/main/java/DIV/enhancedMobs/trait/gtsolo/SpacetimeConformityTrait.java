package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 時空族: 死亡時にそのワールドの時刻を「その日の夜（13000）」に設定する。
 * 1夜1回 — 同じ日 index で発動済みなら他の迎合持ちが死んでも再発動しない。
 */
public final class SpacetimeConformityTrait extends Trait {

    private static final long NIGHT_TIME = 13000L;
    private static final long DAY_LENGTH = 24000L;

    /** ワールドごとの「最後に夜化した日 index」（原典同様 static で揮発）。 */
    private static final Map<UUID, Long> LAST_CONFORMED_DAY = new ConcurrentHashMap<>();

    public SpacetimeConformityTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_conformity", "STCONF", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        World world = mob.getWorld();
        long dayIndex = world.getFullTime() / DAY_LENGTH;
        Long last = LAST_CONFORMED_DAY.get(world.getUID());
        if (last != null && last == dayIndex) {
            return; // 今夜は既に発動済
        }
        // 同日内の 13000 へ設定（深夜なら巻き戻し = 原典同等）
        world.setFullTime(dayIndex * DAY_LENGTH + NIGHT_TIME);
        LAST_CONFORMED_DAY.put(world.getUID(), dayIndex);
    }
}
