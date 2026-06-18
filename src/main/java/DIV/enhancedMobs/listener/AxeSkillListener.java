package DIV.enhancedMobs.listener;

import DIV.attributelib.api.Attributes;
import DIV.attributelib.api.Conditions;
import DIV.attributelib.api.DamageLib;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * 斧のレベリングスキル2種。
 * <ul>
 *   <li><b>たけのこ魔法</b>: 攻撃すると敵を真上に吹き飛ばし（6/8/10/16 ブロック）、
 *       対象のいた足元にたけのこ（竹の苗）を出現させる。</li>
 *   <li><b>旭の弔い</b>: 攻撃が火属性になり（4秒着火）、対象が燃えている限り
 *       受ける炎ダメージ +20/30/50/70%・回復効果を受けられない（不死の蘇生も失敗）。</li>
 * </ul>
 * （オールインはステータス変更のみのためリスナー不要。）
 */
public final class AxeSkillListener implements Listener {

    /** 旭の弔い: 効果の付与元 sourceId（attributelib）。 */
    private static final String ASAHI_SOURCE = "enhancedmobs:skill/asahi_mourning";

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_TAKENOKO);
        if (stage < 0 || ItemEnhancer.isBroken(held) || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        // 高さ h まで打ち上がる初速 v ≈ √(0.16h)（MC の重力 0.08/tick² 近似）
        double height = ItemSkills.TAKENOKO_HEIGHT[stage];
        Vector velocity = victim.getVelocity();
        velocity.setY(Math.sqrt(0.16 * height));
        victim.setVelocity(velocity);

        Block feet = victim.getLocation().getBlock();
        if (feet.getType().isAir()) {
            feet.setType(Material.BAMBOO_SAPLING);
            feet.getWorld().playSound(feet.getLocation(), Sound.BLOCK_BAMBOO_SAPLING_PLACE, 1f, 1f);
        }
    }

    // ---- 旭の弔い ----

    /**
     * 特性処理（NORMAL の MobListener ディスパッチ）より先に回復封印を入れるため LOWEST。
     * 着火 → 燃えている間、炎脆弱（fire_resist 負値）+ 回復封印を維持し、鎮火で解除する。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsahi(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_ASAHI);
        if (stage < 0 || ItemEnhancer.isBroken(held) || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        // 火属性化: フレイム相当の着火（既により長く燃えていれば維持）
        victim.setFireTicks(Math.max(victim.getFireTicks(), ItemSkills.ASAHI_IGNITE_TICKS));
        // 炎脆弱 + 回復封印。BURNING 条件付き + 着火時間ぶんの時限なので、
        // 鎮火中は自動で効果停止し、監視タスクは不要（再ヒットでリフレッシュ）。
        Attributes.removeAll(victim, ASAHI_SOURCE);
        Attributes.addTransient(victim, StandardAttributes.FIRE_RESIST, ASAHI_SOURCE,
                Operation.ADD, -ItemSkills.ASAHI_FIRE_VULN[stage],
                ItemSkills.ASAHI_IGNITE_TICKS, Conditions.BURNING);
        Attributes.addTransient(victim, StandardAttributes.HEAL_MULTIPLIER, ASAHI_SOURCE,
                Operation.MULTIPLY, 0.0,
                ItemSkills.ASAHI_IGNITE_TICKS, Conditions.BURNING);
    }

    // ---- 伽藍洞 ----

    /**
     * 伽藍洞: クリティカル攻撃のたびに、相手の回復倍率を恒久的に減算（不死蘇生も封じる）し、
     * 自分の会心率を同値ぶん 7 秒間（クリのたびにスタック）上昇させる。
     * 会心判定後に読むため MONITOR。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHollowingCrit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)
                || victim instanceof ArmorStand) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_HOLLOWING);
        if (stage < 0 || ItemEnhancer.isBroken(held) || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        if (!event.isCritical() && !DamageLib.wasCritical(event)) {
            return; // クリティカル時のみ発動
        }
        double amount = ItemSkills.HOLLOW_VALUE[stage];
        ItemSkills.hollowingHealSeal(victim, amount);
        ItemSkills.hollowingCritStack(player, amount);
    }
}
