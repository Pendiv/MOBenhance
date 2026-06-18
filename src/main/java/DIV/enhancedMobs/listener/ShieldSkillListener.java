package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 盾のレベリングスキル3種 + 盾の経験値獲得。
 * <ul>
 *   <li><b>ガン盾廃止令</b>: ガード成功でHP回復。4秒以上構え続けると強制クールタイム。</li>
 *   <li><b>効果的な反撃</b>: ガード成功で攻撃者をノックバックし、メインハンド武器で反撃。</li>
 *   <li><b>あべこべ</b>: メインハンドの盾＋右クリックでオフハンドの剣/斧を封印し、その攻撃性能を
 *       一時的にプレイヤーへ宿す（盾をメインハンドに持っている間のみ有効）。封印アイテムを
 *       メインハンドで右クリックすると即終了。多重発動・無限上昇を防止。</li>
 * </ul>
 * 盾の経験値: ガード成功で多め、被弾でも最低限を獲得する。
 */
public final class ShieldSkillListener implements Listener {

    private static final int SHIELD_GUARD_XP = 3;
    private static final int SHIELD_HURT_XP = 1;
    /** 周期タスクの間隔（tick）。 */
    private static final int INTERVAL = 5;

    private final EnhancedMobs plugin;
    private final NamespacedKey sealedKey;
    private final NamespacedKey abekobeAtkKey;
    private final NamespacedKey abekobeSpdKey;

    /** あべこべ発動中のプレイヤー状態。 */
    private final Map<UUID, Abekobe> active = new HashMap<>();
    /** ガン盾廃止令: 連続ガード時間（tick）。 */
    private final Map<UUID, Integer> blockHold = new HashMap<>();

    public ShieldSkillListener(EnhancedMobs plugin) {
        this.plugin = plugin;
        this.sealedKey = new NamespacedKey(plugin, "abekobe_sealed");
        this.abekobeAtkKey = new NamespacedKey(plugin, "abekobe_atk");
        this.abekobeSpdKey = new NamespacedKey(plugin, "abekobe_spd");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, INTERVAL, INTERVAL);
    }

    /** あべこべ状態: 残り時間と、宿した攻撃力・攻撃速度の加算量。 */
    private static final class Abekobe {
        int remaining;
        final double atk;
        final double spd;

        Abekobe(int remaining, double atk, double spd) {
            this.remaining = remaining;
            this.atk = atk;
            this.spd = spd;
        }
    }

    // ---- 盾の経験値 + ガードスキル ----

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EquipmentSlot slot = shieldSlot(player);
        if (slot == null) {
            return;
        }
        ItemStack shield = player.getInventory().getItem(slot);
        if (shield == null || ItemEnhancer.isBroken(shield)) {
            return;
        }
        boolean guarded = event instanceof EntityDamageByEntityEvent byEntity
                && player.isBlocking() && isFrontal(player, byEntity.getDamager());

        ItemEnhancer.grantXp(shield, guarded ? SHIELD_GUARD_XP : SHIELD_HURT_XP);
        player.getInventory().setItem(slot, shield);

        if (!guarded) {
            return;
        }
        String skill = ItemSkills.skillId(shield);
        if (skill == null) {
            return;
        }
        int stage = ItemSkills.activeStage(shield, skill);
        if (stage < 0 || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        if (skill.equals(ItemSkills.SKILL_GUN_SHIELD)) {
            healOnGuard(player, stage);
        } else if (skill.equals(ItemSkills.SKILL_COUNTER)) {
            counter(player, ((EntityDamageByEntityEvent) event).getDamager(), stage);
        }
    }

    private void healOnGuard(Player player, int stage) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + ItemSkills.GUN_SHIELD_HEAL[stage]));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.6, 0), 5, 0.3, 0.3, 0.3);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.3f);
    }

    private void counter(Player player, Entity damager, int stage) {
        if (player.getCooldown(Material.SHIELD) > 0) {
            return;
        }
        LivingEntity attacker = resolveAttacker(damager);
        if (attacker == null || attacker == player) {
            return;
        }
        Vector kb = attacker.getLocation().toVector().subtract(player.getLocation().toVector());
        kb.setY(0);
        if (kb.lengthSquared() > 1.0e-6) {
            kb.normalize().multiply(ItemSkills.COUNTER_KNOCKBACK);
        }
        kb.setY(0.35);
        attacker.setVelocity(attacker.getVelocity().add(kb));

        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double dmg = (atk != null ? atk.getValue() : 1.0) * ItemSkills.COUNTER_DAMAGE_PCT;
        attacker.damage(dmg, player);

        boolean sword = player.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD");
        player.getWorld().playSound(attacker.getLocation(),
                sword ? Sound.ENTITY_PLAYER_ATTACK_SWEEP : Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
        player.setCooldown(Material.SHIELD, ItemSkills.COUNTER_COOLDOWN_TICKS[stage]);
    }

    // ---- あべこべ（発動・即終了） ----

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        // 封印アイテムをメインハンドで右クリック → 即終了（在庫データから復元するため状態に依存しない）。
        if (isSealed(main)) {
            event.setCancelled(true);
            end(player);
            return;
        }
        int stage = ItemSkills.activeStage(main, ItemSkills.SKILL_ABEKOBE);
        if (stage < 0 || ItemSkills.weaponSkillsLocked(player)) {
            return; // メインハンドがあべこべ盾でない / 封印中は無関係
        }
        event.setCancelled(true);
        if (ItemEnhancer.isBroken(main)) {
            Lang.actionbar(player, "emob.skill.broken_use");
            return;
        }
        if (player.getCooldown(Material.SHIELD) > 0) {
            return;
        }
        if (active.containsKey(player.getUniqueId())) {
            Lang.actionbar(player, "emob.skill.abekobe_active");
            return;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!isWeapon(off)) {
            Lang.actionbar(player, "emob.skill.abekobe_need_weapon");
            return;
        }
        sealAndActivate(player, off, stage);
    }

    private void sealAndActivate(Player player, ItemStack weapon, int stage) {
        double atk = attrTotal(weapon, Attribute.ATTACK_DAMAGE);
        double spd = attrTotal(weapon, Attribute.ATTACK_SPEED);

        // 元の武器をシリアライズして封印アメジストへ格納（再起動後も復元できるよう実アイテムに保持）。
        String encoded = Base64.getEncoder().encodeToString(weapon.serializeAsBytes());
        ItemStack seal = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = seal.getItemMeta();
        meta.getPersistentDataContainer().set(sealedKey, PersistentDataType.STRING, encoded);
        meta.displayName(Lang.render("emob.skill.abekobe_seal_name"));
        seal.setItemMeta(meta);
        player.getInventory().setItemInOffHand(seal);

        // プレイヤーへ攻撃性能を付与（transient。再起動で自動消滅＝無限上昇しない）。
        removeAbekobeModifiers(player); // 念のため重複防止
        active.put(player.getUniqueId(), new Abekobe(ItemSkills.ABEKOBE_DURATION_TICKS[stage], atk, spd));
        applyAbekobeModifiers(player, atk, spd);

        player.setCooldown(Material.SHIELD, ItemSkills.ABEKOBE_COOLDOWN_SEC * 20);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4);
        Lang.actionbar(player, "emob.skill.abekobe_sealed");
    }

    /** あべこべ終了: 封印アイテムを元の武器へ復元し、付与した性能を解除する。 */
    private void end(Player player) {
        restoreSealed(player);
        removeAbekobeModifiers(player);
        active.remove(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.1f);
        Lang.actionbar(player, "emob.skill.abekobe_released");
    }

    /** インベントリ内の封印アメジストを探し、元の武器へ戻す。 */
    private void restoreSealed(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            ItemStack it = inv.getItem(i);
            if (isSealed(it)) {
                ItemStack original = decodeSealed(it);
                if (original != null) {
                    inv.setItem(i, original);
                }
                return;
            }
        }
    }

    // ---- 周期処理（あべこべの時間・盾持ち判定 / ガン盾の連続構え） ----

    private void tick() {
        // あべこべ: 残り時間を減らし、メインハンドが盾の間だけ攻撃性能を有効化（持ち替え悪用の防止）。
        for (UUID id : new java.util.ArrayList<>(active.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                active.remove(id); // オフラインは transient も消えるため状態のみ破棄
                continue;
            }
            Abekobe state = active.get(id);
            state.remaining -= INTERVAL;
            if (state.remaining <= 0) {
                end(player);
                continue;
            }
            boolean shieldInMain = player.getInventory().getItemInMainHand().getType() == Material.SHIELD;
            if (shieldInMain) {
                applyAbekobeModifiers(player, state.atk, state.spd);
            } else {
                removeAbekobeModifiers(player);
            }
        }
        // ガン盾廃止令: 連続して構え続けると強制クールタイム。
        for (Player player : Bukkit.getOnlinePlayers()) {
            Integer gunStage = gunShieldStage(player);
            if (gunStage != null && player.isBlocking()) {
                int held = blockHold.merge(player.getUniqueId(), INTERVAL, Integer::sum);
                if (held >= ItemSkills.GUN_SHIELD_HOLD_TICKS) {
                    player.setCooldown(Material.SHIELD, ItemSkills.GUN_SHIELD_FORCED_CD_TICKS[gunStage]);
                    blockHold.remove(player.getUniqueId());
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1.2f);
                }
            } else {
                blockHold.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        blockHold.remove(player.getUniqueId());
        if (active.containsKey(player.getUniqueId())) {
            end(player); // 武器を復元して状態を片付ける（transient は再ログインで消える）
        }
    }

    // ---- ヘルパ ----

    /** 構えに使える強化盾のスロット（オフハンド優先、無ければメインハンド）。無ければ null。 */
    private EquipmentSlot shieldSlot(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD && ItemEnhancer.isEnhanceable(off)) {
            return EquipmentSlot.OFF_HAND;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD && ItemEnhancer.isEnhanceable(main)) {
            return EquipmentSlot.HAND;
        }
        return null;
    }

    /** ガン盾廃止令スキルが有効な盾を構えていればその強化段階、無ければ null。 */
    private Integer gunShieldStage(Player player) {
        if (ItemSkills.weaponSkillsLocked(player)) {
            return null; // 武器スキル封印中はガン盾廃止令も無効
        }
        EquipmentSlot slot = shieldSlot(player);
        if (slot == null) {
            return null;
        }
        ItemStack shield = player.getInventory().getItem(slot);
        if (shield == null || !ItemSkills.SKILL_GUN_SHIELD.equals(ItemSkills.skillId(shield))) {
            return null;
        }
        int stage = ItemSkills.activeStage(shield, ItemSkills.SKILL_GUN_SHIELD);
        return stage < 0 ? null : stage;
    }

    /** 攻撃が前方（盾が防げる向き）から来ているか。 */
    private boolean isFrontal(Player player, Entity damager) {
        Entity source = damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter
                ? shooter : damager;
        Vector to = source.getLocation().toVector().subtract(player.getLocation().toVector());
        to.setY(0);
        if (to.lengthSquared() < 1.0e-6) {
            return true;
        }
        Vector facing = player.getLocation().getDirection();
        facing.setY(0);
        if (facing.lengthSquared() < 1.0e-6) {
            return true;
        }
        return facing.normalize().dot(to.normalize()) > 0;
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    private boolean isWeapon(ItemStack item) {
        if (item == null) {
            return false;
        }
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE");
    }

    /** アイテムが指定属性へ与える加算量の合計（デフォルト + メタのモディファイア）。 */
    private double attrTotal(ItemStack item, Attribute attr) {
        double sum = 0;
        for (AttributeModifier m : item.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND).get(attr)) {
            sum += m.getAmount();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getAttributeModifiers() != null) {
            for (AttributeModifier m : meta.getAttributeModifiers().get(attr)) {
                sum += m.getAmount();
            }
        }
        return sum;
    }

    private void applyAbekobeModifiers(Player player, double atk, double spd) {
        ensureModifier(player, Attribute.ATTACK_DAMAGE, abekobeAtkKey, atk);
        ensureModifier(player, Attribute.ATTACK_SPEED, abekobeSpdKey, spd);
    }

    private void removeAbekobeModifiers(Player player) {
        removeModifier(player, Attribute.ATTACK_DAMAGE, abekobeAtkKey);
        removeModifier(player, Attribute.ATTACK_SPEED, abekobeSpdKey);
    }

    /** 同キーのモディファイアが無ければ加算（冪等）。amount が 0 付近なら付与しない。 */
    private void ensureModifier(Player player, Attribute attr, NamespacedKey key, double amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null || Math.abs(amount) < 1.0e-6) {
            return;
        }
        for (AttributeModifier m : inst.getModifiers()) {
            if (key.equals(m.getKey())) {
                return; // 既に付与済み
            }
        }
        inst.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeModifier(Player player, Attribute attr, NamespacedKey key) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        for (AttributeModifier m : inst.getModifiers()) {
            if (key.equals(m.getKey())) {
                inst.removeModifier(m);
                return;
            }
        }
    }

    private boolean isSealed(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_SHARD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(sealedKey, PersistentDataType.STRING);
    }

    private ItemStack decodeSealed(ItemStack seal) {
        ItemMeta meta = seal.getItemMeta();
        if (meta == null) {
            return null;
        }
        String encoded = meta.getPersistentDataContainer().get(sealedKey, PersistentDataType.STRING);
        if (encoded == null) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("封印武器の復元に失敗: " + ex);
            return null;
        }
    }
}
