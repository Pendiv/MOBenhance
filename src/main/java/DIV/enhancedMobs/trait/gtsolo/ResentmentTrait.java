package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * プレイヤー由来ダメージが50%未満で死亡すると、周囲128ブロックの全プレイヤーに
 * 大量のデバフを配布する（原典 Resentment。「楽な討伐」を許さず正面討伐を促す）。
 *
 * <p>生涯の被ダメージをプレイヤー由来/その他に分類して累積し、死亡時に比率判定。
 * 効果: 弱体化III・移動低下III・採掘疲労III・空腹III を (40+20n) 秒、
 * ウィザーII・盲目I をその 1/4。
 */
public final class ResentmentTrait extends Trait {

    private static final double DEBUFF_RADIUS = 128.0;
    private static final String PLAYER_DMG_KEY = "res_pd";
    private static final String OTHER_DMG_KEY = "res_od";

    public ResentmentTrait(int cost, int weight, int maxRank, int minLevel) {
        super("resentment", "RESENT", cost, weight, maxRank, minLevel);
    }

    /** 被ダメージ累積（軽減後の実ダメージ。プレイヤー由来か否かで分類 — 原典 onDamaged 相当）。 */
    @Override
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
        String key = isPlayerSource(event) ? PLAYER_DMG_KEY : OTHER_DMG_KEY;
        EntityState.addDouble(mob, key, event.getFinalDamage());
    }

    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        double playerDamage = EntityState.getDouble(mob, PLAYER_DMG_KEY, 0);
        double total = playerDamage + EntityState.getDouble(mob, OTHER_DMG_KEY, 0);
        if (total <= 0 || playerDamage / total >= 0.5) {
            return; // プレイヤー由来50%以上 → 通常討伐、発動しない
        }
        int duration = (40 + 20 * rank) * 20; // (40 + 20n) 秒
        double radiusSq = DEBUFF_RADIUS * DEBUFF_RADIUS;
        for (Player player : mob.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(mob.getLocation()) > radiusSq) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 2, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 2, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration / 4, 1, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 4, 0, true, true, true));
        }
    }

    /** ダメージ源がプレイヤー（直接攻撃または飛翔体の射手）か。 */
    private static boolean isPlayerSource(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return false;
        }
        Entity damager = byEntity.getDamager();
        return damager instanceof Player
                || (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player);
    }
}
