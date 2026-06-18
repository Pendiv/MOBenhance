package DIV.enhancedMobs.listener;

import DIV.attributelib.api.DamageElements;
import DIV.attributelib.api.DamageLib;
import DIV.enhancedMobs.i18n.Lang;
import DIV.enhancedMobs.item.ItemEnhancer;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * 剣のレベリングスキル「サンダーボルト」。右クリックで視線先に落雷（crit パーティクルの縦一列で演出）。
 * 水に触れていると単体攻撃から半径 {@link ItemSkills#THUNDER_RANGE} の範囲攻撃になる。
 * ダメージは攻撃力比＋固定加算の雷属性（attributelib の雷ダメージ上昇・耐性が乗る）。
 */
public final class ThunderboltListener implements Listener {

    /** 落雷地点を決めるための視線判定距離。 */
    private static final double TARGET_REACH = 40.0;
    /** 単体時、着弾点からこの半径内の最も近い敵を撃つ（視線が多少ずれても当たるように）。 */
    private static final double SINGLE_RADIUS = 3.0;

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        int stage = ItemSkills.activeStage(held, ItemSkills.SKILL_THUNDERBOLT);
        if (stage < 0 || ItemSkills.weaponSkillsLocked(player)) {
            return;
        }
        event.setCancelled(true);
        if (player.getCooldown(held.getType()) > 0) {
            return;
        }
        if (ItemEnhancer.isBroken(held)) {
            Lang.actionbar(player, "emob.skill.broken_use");
            return;
        }
        player.setCooldown(held.getType(), ItemSkills.THUNDER_COOLDOWN_SEC[stage] * 20);

        AttributeInstance atk = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage = (atk != null ? atk.getValue() : 1.0) * ItemSkills.THUNDER_DAMAGE_PCT[stage]
                + ItemSkills.THUNDER_FLAT[stage];
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        RayTraceResult blockHit = world.rayTraceBlocks(eye, dir, TARGET_REACH, FluidCollisionMode.NEVER, true);
        double reach = blockHit != null ? eye.toVector().distance(blockHit.getHitPosition()) : TARGET_REACH;
        RayTraceResult entHit = world.rayTraceEntities(eye, dir, reach, 0.6, ThunderboltListener::isEnemy);
        Location strike = entHit != null && entHit.getHitEntity() != null
                ? entHit.getHitEntity().getLocation()
                : (blockHit != null ? blockHit.getHitPosition().toLocation(world)
                        : eye.clone().add(dir.clone().multiply(TARGET_REACH)));

        // 落雷は crit パーティクルの柱で表現（実際の落雷エンティティは出さない）。
        column(world, strike);
        world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);

        if (player.isInWater()) {
            // 範囲攻撃（水中）: 着弾点周囲 r=THUNDER_RANGE の敵すべて。
            double r = ItemSkills.THUNDER_RANGE;
            for (Entity e : world.getNearbyEntities(strike, r, r, r)) {
                if (isEnemy(e) && e instanceof LivingEntity target
                        && e.getLocation().distanceSquared(strike) <= r * r) {
                    DamageLib.deal(DamageElements.LIGHTNING, player, target, damage);
                    column(world, target.getLocation());
                }
            }
            return;
        }
        // 単体: 着弾点に最も近い敵1体（半径 SINGLE_RADIUS 内）。視線が多少ずれても当たる。
        LivingEntity nearest = null;
        double bestSq = SINGLE_RADIUS * SINGLE_RADIUS;
        for (Entity e : world.getNearbyEntities(strike, SINGLE_RADIUS, SINGLE_RADIUS, SINGLE_RADIUS)) {
            if (!isEnemy(e) || !(e instanceof LivingEntity le)) {
                continue;
            }
            double sq = e.getLocation().distanceSquared(strike);
            if (sq <= bestSq) {
                bestSq = sq;
                nearest = le;
            }
        }
        if (nearest != null) {
            DamageLib.deal(DamageElements.LIGHTNING, player, nearest, damage);
            column(world, nearest.getLocation());
        }
    }

    /** crit パーティクルを地点の鉛直線上に縦一列で並べる（落雷の柱演出）。 */
    private static void column(World world, Location at) {
        Location base = at.clone();
        for (double y = 0; y <= 6.0; y += 0.3) {
            world.spawnParticle(Particle.CRIT, base.clone().add(0, y, 0), 2, 0.02, 0.0, 0.02, 0.0);
        }
    }

    private static boolean isEnemy(Entity e) {
        return e instanceof Enemy && !(e instanceof ArmorStand);
    }
}
