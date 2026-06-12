package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.trait.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * 時空族: プレイヤーに与えたダメージ分だけ「隠蔽」を与える — 与ダメ2HPごとに1段、1段につき
 * 最大HPを1ハート（2HP）削る（最大50段=−100）。持続は2分で被弾のたびにリフレッシュ。
 * 隠された体力は、ポーション効果由来でない回復2HPごとに1段ずつ返済される（端数は繰り越し）。
 * rank 非依存（maxRank 1）。原典の ConcealmentEffect を PDC + 属性モディファイアで再現する。
 */
public final class SpacetimeShadowRaidTrait extends Trait {

    /** 隠蔽の持続（原典 DURATION = 2分。被弾でリフレッシュ）。 */
    private static final int DURATION = 2400;
    /** 最大段数（原典 amplifier 49 = 50段 → 最大HP −100）。 */
    private static final int MAX_STACKS = 50;

    private static final NamespacedKey HP_KEY = new NamespacedKey(EnhancedMobs.get(), "trait_st_raid_conceal");

    public SpacetimeShadowRaidTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_shadow_raid", "STRAID", cost, weight, maxRank, minLevel);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player)) {
            return;
        }
        double dmg = event.getFinalDamage();
        if (dmg <= 0) {
            return;
        }
        int add = Math.max(1, (int) Math.round(dmg / 2.0));  // 2HP ごとに 1 段
        apply(player, Math.min(currentStacks(player) + add, MAX_STACKS));
        EntityState.setFlag(player, "conceal", DURATION);    // 持続は常に2分へリフレッシュ
        registerExpiryWatcher(player);
    }

    /**
     * {@code HealListener} から回復確定時に呼ばれる。ポーション効果由来でない回復2HPごとに
     * 隠蔽を1段返済する（端数は PDC に累積して繰り越し。持続は減らさない）。
     */
    public static void onHeal(LivingEntity entity, EntityRegainHealthEvent event) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int stacks = currentStacks(player);
        if (stacks <= 0) {
            return;
        }
        // 原典: 再生効果中の回復は返済対象外（hasEffect(REGENERATION) 判定 + 回復理由の併用）。
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN
                || player.hasPotionEffect(PotionEffectType.REGENERATION)) {
            return;
        }
        double accum = EntityState.addDouble(player, "conceal_heal_accum", event.getAmount());
        int repay = (int) (accum / 2.0);
        if (repay <= 0) {
            return;
        }
        EntityState.setDouble(player, "conceal_heal_accum", accum - repay * 2.0);
        int next = stacks - repay;
        if (next > 0) {
            apply(player, next);
        } else {
            clear(player);
        }
    }

    /** 現在の有効段数。期限切れなら0扱い（後始末は監視レーンが行う）。 */
    private static int currentStacks(Player player) {
        return EntityState.hasFlag(player, "conceal") ? EntityState.getInt(player, "conceal_stacks", 0) : 0;
    }

    /** 段数を保存し、最大HP −2×段 のモディファイアを貼り替える。超過HPはクランプ。 */
    private static void apply(Player player, int stacks) {
        EntityState.setInt(player, "conceal_stacks", stacks);
        Mobs.addModifier(player, Attribute.MAX_HEALTH, HP_KEY,
                -2.0 * stacks, AttributeModifier.Operation.ADD_NUMBER);
        double max = Mobs.maxHealth(player);
        if (player.getHealth() > max) {
            player.setHealth(Math.max(1.0, max));
        }
        // バニラのポーションアイコンが無いため、actionbar で段数を通知する（原典との表示差の代替）。
        player.sendActionBar(Component.text("隠蔽 " + stacks + "段（最大HP -" + stacks + "♥）",
                NamedTextColor.DARK_PURPLE));
    }

    /** 隠蔽を完全に解除する。 */
    private static void clear(Player player) {
        EntityState.setInt(player, "conceal_stacks", 0);
        EntityState.setDouble(player, "conceal_heal_accum", 0);
        AttributeInstance inst = player.getAttribute(Attribute.MAX_HEALTH);
        if (inst != null) {
            inst.getModifiers().stream().filter(m -> HP_KEY.equals(m.getKey())).toList()
                    .forEach(inst::removeModifier);
        }
    }

    /** 期限切れ（2分無被弾）で最大HP削りを解除する監視レーン（プレイヤーごとに1つ）。 */
    private static void registerExpiryWatcher(Player player) {
        if (FastTick.isRegistered(player, "conceal")) {
            return;
        }
        FastTick.register(player, "conceal", () -> {
            if (!player.isOnline()) {
                return false;
            }
            if (Bukkit.getCurrentTick() % 20 != 0) {
                return true;
            }
            if (currentStacks(player) <= 0) {
                clear(player);
                return false;
            }
            return true;
        });
    }
}
