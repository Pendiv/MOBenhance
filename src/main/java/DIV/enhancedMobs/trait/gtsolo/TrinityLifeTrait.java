package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 三体生命: 30秒ごとに {物理反射 / 魔法生態 / 不死} から1モードを抽選して循環する
 * （切替には周囲32mにプレイヤーが必要。期限切れでもプレイヤー不在の間は前モードを維持 = 原典準拠）。
 * モード2・3は移植済み特性を内部付与し、切替時に返却する（元から持っていた分は触らない）。
 * 周囲16mに別の三体生命が居ると共鳴し、最大体力 +(20+10N)% / 毎秒1.0回復 / 再生V(60t) を得る。
 */
public final class TrinityLifeTrait extends Trait {

    /** モードの持続時間（原典 600t = 30秒）。 */
    private static final int MODE_DURATION_TICKS = 600;
    /** モード切替に必要なプレイヤーの索敵範囲。 */
    private static final double PLAYER_RANGE = 32.0;
    /** 共鳴の検知範囲。 */
    private static final double RESONANCE_RANGE = 16.0;
    /** モード1: 直接物理ダメージの反射率。 */
    private static final double REFLECT_RATIO = 0.5;

    /** 反射の再入ガード（三体生命同士の殴り合いでの無限反射防止。原典 Phase15Handler と同じ構造）。 */
    private static boolean reflecting = false;

    private final NamespacedKey resonanceKey;

    public TrinityLifeTrait(int cost, int weight, int maxRank, int minLevel) {
        super("trinity_life", "TRINITY", cost, weight, maxRank, minLevel);
        this.resonanceKey = new NamespacedKey(EnhancedMobs.get(), "trait_trinity_resonance");
    }

    /** 現在のモード（0=未抽選, 1=物理反射, 2=魔法生態, 3=不死）。 */
    private static int mode(LivingEntity mob) {
        return EntityState.getInt(mob, "trinity_mode", 0);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        rollMode(mob, rank);
        tickResonance(mob, rank);
    }

    /** モード1: 受けた直接物理ダメージの50%を攻撃者へ反射する。 */
    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker,
                             EntityDamageByEntityEvent event) {
        if (reflecting || attacker == null || mode(mob) != 1) {
            return;
        }
        // 直接物理のみ（damager が攻撃者本体 = 飛翔体・間接ソースは対象外）
        if (event.getDamager() != attacker) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }
        reflecting = true;
        try {
            attacker.damage(event.getDamage() * REFLECT_RATIO, mob);
        } finally {
            reflecting = false;
        }
    }

    /** 期限切れかつ周囲32mにプレイヤーが居るとき、前回の内部付与を返却して1〜3を再抽選する。 */
    private void rollMode(LivingEntity mob, int rank) {
        if (EntityState.hasFlag(mob, "trinity_until") || !hasNearbyPlayer(mob)) {
            return;
        }
        EnhancedMobs plugin = EnhancedMobs.get();
        clearGrants(mob, plugin);
        int next = 1 + ThreadLocalRandom.current().nextInt(3);
        EntityState.setInt(mob, "trinity_mode", next);
        EntityState.setFlag(mob, "trinity_until", MODE_DURATION_TICKS);
        if (next == 2 && !hasTrait(mob, "magical_creatures")) {
            // 原典: MAGICAL_CREATURES を level で内部獲得（移植側は maxRank に丸められる）
            if (plugin.traits().addTrait(mob, "magical_creatures", rank)) {
                EntityState.setInt(mob, "trinity_granted_mc", 1);
            }
        } else if (next == 3 && !hasTrait(mob, "undying")) {
            // 不死は回数無制限なので、モード3の間付与するだけでよい（消費フラグ管理は不要）。
            if (plugin.traits().addTrait(mob, "undying", 1)) {
                EntityState.setInt(mob, "trinity_granted_undying", 1);
            }
        }
    }

    /** 抽選で内部付与した特性を返却する（元から持っていた分は触らない）。 */
    private void clearGrants(LivingEntity mob, EnhancedMobs plugin) {
        if (EntityState.getInt(mob, "trinity_granted_mc", 0) != 0) {
            plugin.traits().stripTrait(mob, "magical_creatures");
            EntityState.setInt(mob, "trinity_granted_mc", 0);
        }
        if (EntityState.getInt(mob, "trinity_granted_undying", 0) != 0) {
            plugin.traits().stripTrait(mob, "undying");
            EntityState.setInt(mob, "trinity_granted_undying", 0);
        }
        EntityState.setInt(mob, "trinity_mode", 0);
    }

    /** 共鳴: 周囲16mの別の三体生命を検知して最大体力ボーナスをトグルし、継続回復を行う。 */
    private void tickResonance(LivingEntity mob, int rank) {
        boolean resonating = mob.getNearbyEntities(RESONANCE_RANGE, RESONANCE_RANGE, RESONANCE_RANGE)
                .stream()
                .anyMatch(e -> e instanceof LivingEntity other && other.isValid() && hasTrait(other, id()));
        AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            boolean has = hp.getModifiers().stream().anyMatch(m -> resonanceKey.equals(m.getKey()));
            if (resonating && !has) {
                // 原典: MAX_HEALTH ×(1 + 0.20 + 0.10×level)（MULTIPLY_TOTAL 相当）
                Mobs.addModifier(mob, Attribute.MAX_HEALTH, resonanceKey,
                        0.20 + 0.10 * rank, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            } else if (!resonating && has) {
                hp.getModifiers().stream().filter(m -> resonanceKey.equals(m.getKey()))
                        .toList().forEach(hp::removeModifier);
                if (mob.getHealth() > hp.getValue()) {
                    mob.setHealth(hp.getValue());   // 解除時 HP clamp
                }
            }
        }
        if (resonating) {
            // 毎秒 1.0 回復（tick間隔換算）＋ 再生V 60t
            double scale = Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval) / 20.0;
            mob.setHealth(Math.min(Mobs.maxHealth(mob), mob.getHealth() + 1.0 * scale));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 4, true, false, false));
        }
    }

    private static boolean hasNearbyPlayer(LivingEntity mob) {
        return mob.getNearbyEntities(PLAYER_RANGE, PLAYER_RANGE, PLAYER_RANGE).stream()
                .anyMatch(e -> e instanceof Player player && player.getGameMode() != GameMode.SPECTATOR);
    }

    private static boolean hasTrait(Entity entity, String id) {
        return entity instanceof LivingEntity living && MobData.of(living).isProcessed()
                && EnhancedMobs.get().traits().read(living).keySet().stream()
                        .anyMatch(t -> t.id().equals(id));
    }
}
