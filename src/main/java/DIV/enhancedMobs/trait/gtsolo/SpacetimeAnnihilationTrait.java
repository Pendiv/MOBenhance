package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 時空族: プレイヤーを殺害しようとしてトーテムに阻まれると「確殺」に覚醒する。
 * 最初のトーテム復活は成立するが、その代償として保持者は発光し攻撃力 +50% を得て、
 * 以後の致死打ではトーテム復活を封じる（原典の CertainKillEffect + PlayerTotemMixin 相当）。
 */
public final class SpacetimeAnnihilationTrait extends Trait {

    /** 致死打を与えた本特性持ちの UUID（被弾者の PDC に短時間記録）。 */
    private static final NamespacedKey KILLER_KEY = new NamespacedKey(EnhancedMobs.get(), "st_anni_killer");
    /** 致死打記録の有効ウィンドウ（同 tick 内のトーテム判定には十分）。 */
    private static final int WINDOW_TICKS = 10;

    public SpacetimeAnnihilationTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_annihilation", "STANNI", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player) || target.getHealth() - event.getFinalDamage() > 0) {
            return;
        }
        if (EntityState.hasFlag(mob, "certain_kill")) {
            // 覚醒済み: トーテム復活を封じ、今度こそ確実に殺害する。
            EntityState.setFlag(target, "deny_resurrect", 40);
        } else {
            // 未覚醒: この致死打の攻撃者を短時間記録。トーテム復活が成立したら覚醒する。
            target.getPersistentDataContainer().set(KILLER_KEY, PersistentDataType.STRING,
                    mob.getUniqueId().toString());
            EntityState.setFlag(target, "st_anni_window", WINDOW_TICKS);
        }
    }

    /**
     * {@code ResurrectListener} から蘇生成立時に呼ばれる。
     * 直前の致死打の攻撃者が本特性持ちなら「確殺」に覚醒させる。
     */
    public static void onResurrected(LivingEntity revived) {
        if (!EntityState.hasFlag(revived, "st_anni_window")) {
            return;
        }
        String raw = revived.getPersistentDataContainer().get(KILLER_KEY, PersistentDataType.STRING);
        if (raw == null) {
            return;
        }
        Entity attacker = Bukkit.getEntity(UUID.fromString(raw));
        if (!(attacker instanceof LivingEntity living) || !living.isValid()) {
            return;
        }
        boolean holds = EnhancedMobs.get().traits().read(living).keySet().stream()
                .anyMatch(t -> t.id().equals("spacetime_annihilation"));
        if (!holds || EntityState.hasFlag(living, "certain_kill")) {
            return;
        }
        // 覚醒: 実質永続の確殺フラグ + 発光 + 攻撃力 +50%（MULTIPLY_BASE ≒ MULTIPLY_SCALAR_1）。
        EntityState.setFlag(living, "certain_kill", Integer.MAX_VALUE);
        living.setGlowing(true);
        Mobs.addModifier(living, Attribute.ATTACK_DAMAGE,
                new NamespacedKey(EnhancedMobs.get(), "trait_st_anni_atk"),
                0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
}
