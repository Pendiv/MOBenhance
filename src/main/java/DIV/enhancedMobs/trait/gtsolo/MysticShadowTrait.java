package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 神秘の影 — プレイヤーに与ダメするたび、その攻撃力/移動速度/防御力の現在値の
 * <b>(1+0.3n)%</b> を奪う（プレイヤーから減算 ⇔ mob へ付与が同量で対）。最大 7 回累積。
 * mob が倒されるか、2 分間互いに無交戦だと失効し、奪ったステータスをプレイヤーへ返還する。
 *
 * <p>プレイヤー側の減算は transient modifier（NBT 非永続）のため、万一返還漏れがあっても
 * 再ログイン/再起動で確実に消える（= ステータスが恒久に失われない）。mob のデスポーン・
 * チャンクアンロード時は FastTick の監視が即時返還する（mob 側に残った加算は
 * 再ロード後の 2 分タイムアウトで自浄される）。
 */
public final class MysticShadowTrait extends Trait {

    private static final Attribute[] ATTRS = {Attribute.ATTACK_DAMAGE, Attribute.MOVEMENT_SPEED, Attribute.ARMOR};
    private static final String[] NAMES = {"atk", "move", "armor"};
    private static final int MAX_STACKS = 7;
    /** 最終交戦からの失効時間（2 分）。ワールド fullTime 基準で再起動を跨いでも安全。 */
    private static final long TIMEOUT_TICKS = 2400L;
    private static final String WATCH_KEY = "mystic_shadow";
    private static final int WATCH_INTERVAL = 20;

    /** 奪取対象プレイヤーの UUID（mob の PDC に保存）。 */
    private static final NamespacedKey TARGET_KEY = new NamespacedKey(EnhancedMobs.get(), "shadow_target");

    public MysticShadowTrait(int cost, int weight, int maxRank, int minLevel) {
        super("mystic_shadow", "MYSTIC", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player)) {
            return;
        }
        EntityState.setDouble(mob, "shadow_last", mob.getWorld().getFullTime());
        int stacks = EntityState.getInt(mob, "shadow_stacks", 0);
        if (stacks >= MAX_STACKS) {
            return; // 上限到達後は交戦時刻の更新のみ。
        }
        // 奪取対象が替わった場合は旧対象へ先に返還する（減算の二重残留防止）。
        String prev = mob.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
        if (prev != null && !prev.equals(player.getUniqueId().toString())) {
            Player old = Bukkit.getPlayer(UUID.fromString(prev));
            if (old != null) {
                removePlayerModifiers(mob, old);
            }
        }

        double pct = 0.01 + 0.003 * rank;
        for (int i = 0; i < ATTRS.length; i++) {
            AttributeInstance inst = player.getAttribute(ATTRS[i]);
            if (inst == null) {
                continue;
            }
            // 現在値（既奪取分の減算込み）× pct を累計へ加算し、両者の modifier を貼り直す。
            double total = EntityState.addDouble(mob, "shadow_" + NAMES[i], inst.getValue() * pct);
            applyTransient(player, ATTRS[i], playerKey(mob, i), -total);
            Mobs.addModifier(mob, ATTRS[i], selfKey(i), total, AttributeModifier.Operation.ADD_NUMBER);
        }
        EntityState.setInt(mob, "shadow_stacks", stacks + 1);
        mob.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING,
                player.getUniqueId().toString());
        startWatcher(mob);
    }

    /** 2 分間無交戦で失効 → 返還。 */
    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "shadow_stacks", 0) <= 0) {
            return;
        }
        long last = (long) EntityState.getDouble(mob, "shadow_last", 0);
        if (mob.getWorld().getFullTime() - last > TIMEOUT_TICKS) {
            restore(mob);
        }
    }

    /** mob 死亡時は必ず返還する。 */
    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        restore(mob);
    }

    /** 奪ったステータスをプレイヤーへ返還し、両側の modifier と累積状態を除去する。冪等。 */
    private static void restore(LivingEntity mob) {
        String raw = mob.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
        if (raw != null) {
            Player player = Bukkit.getPlayer(UUID.fromString(raw));
            if (player != null) {
                removePlayerModifiers(mob, player);
            }
        }
        for (int i = 0; i < ATTRS.length; i++) {
            removeByKey(mob, ATTRS[i], selfKey(i));
            EntityState.setDouble(mob, "shadow_" + NAMES[i], 0);
        }
        EntityState.setInt(mob, "shadow_stacks", 0);
        mob.getPersistentDataContainer().remove(TARGET_KEY);
    }

    /**
     * デスポーン・チャンクアンロードの保険: mob が無効になったら即時返還する
     * （死亡時は onDeath が先に返還済みのため no-op）。
     */
    private static void startWatcher(LivingEntity mob) {
        if (FastTick.isRegistered(mob, WATCH_KEY)) {
            return;
        }
        int[] countdown = {WATCH_INTERVAL};
        FastTick.register(mob, WATCH_KEY, () -> {
            if (!mob.isValid()) {
                restore(mob);
                return false;
            }
            if (--countdown[0] > 0) {
                return true;
            }
            countdown[0] = WATCH_INTERVAL;
            return EntityState.getInt(mob, "shadow_stacks", 0) > 0; // 返還済みなら解除。
        });
    }

    private static void removePlayerModifiers(LivingEntity mob, Player player) {
        for (int i = 0; i < ATTRS.length; i++) {
            removeByKey(player, ATTRS[i], playerKey(mob, i));
        }
    }

    /** transient modifier（NBT 非永続 = 再起動でデバフが残留しない）を冪等に貼り直す。 */
    private static void applyTransient(LivingEntity entity, Attribute attr, NamespacedKey key, double amount) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
        inst.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private static void removeByKey(LivingEntity entity, Attribute attr, NamespacedKey key) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) {
            inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
        }
    }

    /** プレイヤー側のキーは mob UUID を含める（複数の影持ちと交戦しても衝突しない）。 */
    private static NamespacedKey playerKey(LivingEntity mob, int idx) {
        return new NamespacedKey(EnhancedMobs.get(), "shadow_" + NAMES[idx] + "_" + mob.getUniqueId());
    }

    private static NamespacedKey selfKey(int idx) {
        return new NamespacedKey(EnhancedMobs.get(), "trait_shadow_" + NAMES[idx]);
    }
}
