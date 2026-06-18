package DIV.enhancedMobs.level;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.config.MainConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * レベルに応じたステータス強化を適用。個体差を生むランダム配分を行う。
 *
 * <p>HP・防御・攻撃・速度の4ステータスに {@code count * perStep} を加算。
 * count の決め方:
 * <ul>
 *   <li>細粒部 {@code ones = N % 10}: ランダムなステータスに +1 を ones 回。</li>
 *   <li>粗粒部 {@code high = N / 10}: ランダムなステータスに +high を {@code coarseDraws}（≈10）回。</li>
 * </ul>
 * 合計 {@code 10*high + ones = N} になるが、抽選は ≈19 回のみ。
 * あえて抽選を荒くし、個体差を調整。
 */
public final class LevelScaler {

    private static final int HEALTH = 0;
    private static final int ARMOR = 1;
    private static final int ATTACK = 2;
    private static final int SPEED = 3;

    private final MainConfig config;
    private final NamespacedKey healthKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey attackKey;
    private final NamespacedKey speedKey;

    public LevelScaler(EnhancedMobs plugin, MainConfig config) {
        this.config = config;
        this.healthKey = new NamespacedKey(plugin, "enh_health");
        this.armorKey = new NamespacedKey(plugin, "enh_armor");
        this.attackKey = new NamespacedKey(plugin, "enh_attack");
        this.speedKey = new NamespacedKey(plugin, "enh_speed");
    }

    public void apply(LivingEntity mob, int level) {
        int[] counts = allocate(level);

        applyFlat(mob, Attribute.MAX_HEALTH, healthKey, counts[HEALTH] * config.enhHealth);
        applyFlat(mob, Attribute.ARMOR, armorKey, counts[ARMOR] * config.enhArmor);
        applyFlat(mob, Attribute.ATTACK_DAMAGE, attackKey, counts[ATTACK] * config.enhAttack);

        double speedBonus = Math.min(counts[SPEED] * config.enhSpeed, config.maxSpeedBonus);
        applyFlat(mob, Attribute.MOVEMENT_SPEED, speedKey, speedBonus);

        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            mob.setHealth(maxHealth.getValue());
        }
    }

    /** レベリングで加算された近接攻撃力ボーナス（{@code enh_attack} モディファイア量）。無ければ 0。 */
    public double meleeAttackBonus(LivingEntity mob) {
        AttributeInstance inst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return 0.0;
        }
        return inst.getModifiers().stream()
                .filter(m -> attackKey.equals(m.getKey()))
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    private int[] allocate(int level) {
        int[] counts = new int[4];
        int ones = level % 10;
        int high = level / 10;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (high > 0) {
            for (int i = 0; i < config.coarseDraws; i++) {
                counts[random.nextInt(4)] += high;
            }
        }
        for (int i = 0; i < ones; i++) {
            counts[random.nextInt(4)] += 1;
        }
        return counts;
    }

    private void applyFlat(LivingEntity mob, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) {
            return; // クリーパーや遠距離専用モブは ATTACK_DAMAGE 属性を持たないため
        }
        inst.getModifiers().stream()
                .filter(m -> key.equals(m.getKey()))
                .toList()
                .forEach(inst::removeModifier);
        if (amount != 0) {
            inst.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
