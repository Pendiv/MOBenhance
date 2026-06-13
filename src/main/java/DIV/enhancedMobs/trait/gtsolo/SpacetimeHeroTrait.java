package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 時空族: 初期状態は無効。周囲24mで時空mobが死ぬたびにカウントが貯まり、閾値 3+2n に達すると覚醒
 * — 全快し攻撃力 +(125+25N)%（MULTIPLY_BASE）を固定10秒間得る（1回きり）。
 * 覚醒しないまま120秒（2400 tick）を超えると特性自体が消滅する。
 */
public final class SpacetimeHeroTrait extends Trait {

    /** 覚醒の期限（原典 DEADLINE_TICKS = 120秒）。 */
    private static final int DEADLINE_TICKS = 2400;
    /** 覚醒バーストの持続（原典 BUFF_TICKS = 10秒）。 */
    private static final int BUFF_TICKS = 200;
    /** 死亡カウントを拾う半径（原典 RADIUS）。 */
    private static final double RADIUS = 24.0;

    private static final NamespacedKey ATK_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_hero_atk");

    public SpacetimeHeroTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_hero", "STHERO", cost, weight, maxRank, minLevel);
    }

    /** 覚醒に必要な近隣時空mobの死亡数（原典 3 + 2n）。 */
    private static int threshold(int rank) {
        return 3 + 2 * rank;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
        // 開始時刻は初回のみ記録（rank昇格などの再initializeで期限を引き延ばさない）。
        if (EntityState.getInt(mob, "hero_start", -1) < 0) {
            EntityState.setInt(mob, "hero_start", (int) EntityState.gameTime());
        }
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "hero_on", 0) == 1) {
            // 覚醒済み: 10秒バーストの期限が切れたら攻撃力バフを除去（以後は何もしない）。
            if (!EntityState.hasFlag(mob, "hero_buff")) {
                removeModifier(mob, ATK_KEY);
            }
            return;
        }
        int start = EntityState.getInt(mob, "hero_start", -1);
        if (start < 0) {
            EntityState.setInt(mob, "hero_start", (int) EntityState.gameTime());
            return;
        }
        if ((int) EntityState.gameTime() - start > DEADLINE_TICKS) {
            // 期限切れ: 特性そのものが消滅する。
            EnhancedMobs.get().traits().stripTrait(mob, id());
        }
    }

    /**
     * {@code MobListener.onDeath} から、spacetime タグ持ちの死亡時に呼ばれる。
     * 半径24m内の未覚醒の英雄たちのカウントを進め、閾値到達で覚醒させる（原典 onAnyDeath）。
     */
    public static void onSpacetimeDeath(LivingEntity dead) {
        for (Entity e : dead.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(e instanceof LivingEntity hero) || hero.isDead()
                    || EntityState.getInt(hero, "hero_on", 0) == 1) {
                continue;
            }
            int rank = heroRank(hero);
            if (rank <= 0) {
                continue;
            }
            if (EntityState.addInt(hero, "hero_count", 1) >= threshold(rank)) {
                enable(hero, rank);
            }
        }
    }

    /** 覚醒: 全快 + 攻撃力 +(125+25N)%（MULTIPLY_BASE = ADD_SCALAR）を10秒間。 */
    private static void enable(LivingEntity hero, int rank) {
        EntityState.setInt(hero, "hero_on", 1);
        hero.setHealth(Mobs.maxHealth(hero));
        Mobs.addModifier(hero, Attribute.ATTACK_DAMAGE, ATK_KEY,
                1.25 + 0.25 * rank, AttributeModifier.Operation.ADD_SCALAR);
        EntityState.setFlag(hero, "hero_buff", BUFF_TICKS);
    }

    private static int heroRank(LivingEntity mob) {
        for (Map.Entry<Trait, Integer> entry : EnhancedMobs.get().traits().read(mob).entrySet()) {
            if (entry.getKey().id().equals("spacetime_hero")) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private static void removeModifier(LivingEntity mob, NamespacedKey key) {
        AttributeInstance inst = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
    }
}
