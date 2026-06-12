package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 防具のレベリングスキル4種。
 * <ul>
 *   <li><b>ジャストブロック</b>: 攻撃を受けると一定確率（25/30/35/50%、段階 0〜3）で完全にブロックする。
 *       複数部位に付いていても効果は一様（最大段階のみ採用、確率は累積しない）。</li>
 *   <li><b>デバフ免疫</b>: 有害ポーション効果の付与を拒否する。CT 30/28/26/15 秒。</li>
 *   <li><b>獅子の心臓</b>（チェストプレート限定）: HP が最大の10%以下になる被弾でトーテム相当の
 *       延命効果 + 耐性V を発動。CT 150/140/130/100 秒。</li>
 *   <li><b>過去からの贈り物</b>: 死亡時に現在の防御力の25%を「未来へ送る」。次のリスポーンで
 *       防御力上昇として受け取る。受領中に死亡すると効果は消え、その死亡では送らない。</li>
 * </ul>
 */
public final class ArmorSkillListener implements Listener {

    private final EnhancedMobs plugin;
    /** 過去からの贈り物: リスポーン待ちの送付額（プレイヤー PDC、死後も永続）。 */
    private final NamespacedKey pastGiftPending;
    /** 過去からの贈り物: 受領中の防御力加算額（プレイヤー PDC）。 */
    private final NamespacedKey pastGiftActive;
    /** 過去からの贈り物: 防御力モディファイアのキー。 */
    private final NamespacedKey pastGiftModifier;

    /** デバフ免疫: プレイヤー UUID → CT 期限（ms）。 */
    private final Map<UUID, Long> debuffCooldown = new HashMap<>();
    /** 獅子の心臓: プレイヤー UUID → CT 期限（ms）。 */
    private final Map<UUID, Long> lionCooldown = new HashMap<>();

    public ArmorSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
        this.pastGiftPending = new NamespacedKey(plugin, "past_gift_pending");
        this.pastGiftActive = new NamespacedKey(plugin, "past_gift_active");
        this.pastGiftModifier = new NamespacedKey(plugin, "past_gift_armor");
    }

    // ---- ジャストブロック ----

    @EventHandler(ignoreCancelled = true)
    public void onAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int best = bestArmorStage(player, ItemSkills.SKILL_JUST_BLOCK);
        if (best < 0 || ThreadLocalRandom.current().nextDouble() >= ItemSkills.JUST_BLOCK_CHANCE[best]) {
            return;
        }
        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        player.sendActionBar(Component.text("ジャストブロック！", NamedTextColor.AQUA));
    }

    // ---- デバフ免疫 ----

    /** 有害効果の付与（ADDED/CHANGED）を、CT が空いていればキャンセルして拒否する。 */
    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action != EntityPotionEffectEvent.Action.ADDED
                && action != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || newEffect.getType().getCategory() != PotionEffectTypeCategory.HARMFUL) {
            return;
        }
        int best = bestArmorStage(player, ItemSkills.SKILL_DEBUFF_IMMUNITY);
        if (best < 0 || onCooldown(debuffCooldown, player)) {
            return;
        }
        event.setCancelled(true);
        startCooldown(debuffCooldown, player, ItemSkills.DEBUFF_COOLDOWN_SEC[best]);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
        player.sendActionBar(Component.text("デバフ免疫！", NamedTextColor.AQUA));
    }

    // ---- 獅子の心臓 ----

    /** HP が最大の10%以下になる被弾で発動。致死分は半ハート残しに抑え、トーテム相当の効果を付与。 */
    @EventHandler(ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || ItemEnhancer.isBroken(chest)) {
            return;
        }
        int stage = ItemSkills.activeStage(chest, ItemSkills.SKILL_LION_HEART);
        if (stage < 0) {
            return;
        }
        double health = player.getHealth();
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        if (health - event.getFinalDamage() > max * 0.10) {
            return;
        }
        if (onCooldown(lionCooldown, player)) {
            return;
        }
        startCooldown(lionCooldown, player, ItemSkills.LION_COOLDOWN_SEC[stage]);
        if (health - event.getFinalDamage() <= 0) {
            // 致死: ダメージを抑えて半ハート残す（トーテムの死亡回避相当）
            event.setDamage(Math.max(0, health - 1));
        }
        // バニラトーテム相当 + 耐性V
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.3);
        player.sendActionBar(Component.text("獅子の心臓が発動した！", NamedTextColor.GOLD));
    }

    // ---- 過去からの贈り物 ----

    /**
     * 死亡時の送付判定。受領中（効果あり）の死亡は効果を消費するだけで送らない。
     * 未受領なら、スキル有効な防具を装備していた場合に現在の防御力 × 25% を未来へ送る。
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pdc.has(pastGiftActive, PersistentDataType.DOUBLE)) {
            pdc.remove(pastGiftActive);
            removePastGiftModifier(player);
            return;
        }
        if (bestArmorStage(player, ItemSkills.SKILL_PAST_GIFT) < 0) {
            return;
        }
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        double amount = (armor == null ? 0 : armor.getValue()) * ItemSkills.PAST_GIFT_RATIO;
        if (amount > 0) {
            pdc.set(pastGiftPending, PersistentDataType.DOUBLE, amount);
        }
    }

    /** リスポーン1tick後（属性リセット完了後）に送付額を受領し、防御力モディファイアを付ける。 */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            Double pending = pdc.get(pastGiftPending, PersistentDataType.DOUBLE);
            if (pending == null || !player.isOnline()) {
                return;
            }
            pdc.remove(pastGiftPending);
            pdc.set(pastGiftActive, PersistentDataType.DOUBLE, pending);
            applyPastGiftModifier(player, pending);
            player.sendMessage(Component.text(
                    String.format("過去からの贈り物を受け取った（防御力 +%.1f）", pending),
                    NamedTextColor.GOLD));
        });
    }

    /** transient モディファイアはログアウトで消えるため、受領中なら再ログイン時に付け直す。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Double active = player.getPersistentDataContainer().get(pastGiftActive, PersistentDataType.DOUBLE);
        if (active != null) {
            applyPastGiftModifier(player, active);
        }
    }

    private void applyPastGiftModifier(Player player, double amount) {
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        if (armor == null) {
            return;
        }
        removePastGiftModifier(player);
        armor.addTransientModifier(new AttributeModifier(
                pastGiftModifier, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removePastGiftModifier(Player player) {
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        if (armor == null) {
            return;
        }
        for (AttributeModifier m : armor.getModifiers()) {
            if (pastGiftModifier.equals(m.getKey())) {
                armor.removeModifier(m);
                return;
            }
        }
    }

    // ---- 共通 ----

    /** 装備4部位を走査し、指定スキルの最大段階を返す（破壊寸前は除外、無ければ -1）。 */
    private int bestArmorStage(Player player, String skillId) {
        int best = -1;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            int stage = ItemSkills.activeStage(piece, skillId);
            if (stage >= 0 && !ItemEnhancer.isBroken(piece)) {
                best = Math.max(best, stage);
            }
        }
        return best;
    }

    private boolean onCooldown(Map<UUID, Long> map, Player player) {
        Long until = map.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private void startCooldown(Map<UUID, Long> map, Player player, int seconds) {
        map.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }
}
