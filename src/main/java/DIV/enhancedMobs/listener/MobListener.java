package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.PlayerData;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** スポーン・戦闘・死亡イベントをレベリング・特性・ヘッド表示に繋ぐ。 */
public final class MobListener implements Listener {

    private final EnhancedMobs plugin;

    public MobListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.mainConfig().levelingEnabled) {
            return;
        }
        LivingEntity entity = event.getEntity();
        // Monster は既定で処理対象。非 Monster のボス（エンダードラゴンなど）は
        // per-mob ボーナスが設定されている場合にのみ対象となる。
        if (!(entity instanceof Monster) && !plugin.mobBonus().has(entity.getType())) {
            return;
        }
        if (MobData.of(entity).isProcessed()) {
            return;
        }
        if (!plugin.dimensions().isEnabled(entity.getWorld())) {
            return;
        }
        int level = plugin.difficulty().compute(entity.getLocation());
        level = plugin.mobBonus().apply(entity.getType(), level);
        plugin.initializeMob(entity, level);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim && MobData.of(victim).isProcessed()) {
            plugin.traits().onAttacked(victim, event);
            LivingEntity attacker = resolveAttacker(event.getDamager());
            if (attacker != null) {
                plugin.traits().onAttackedBy(victim, attacker, event);
            }
        }
        if (event.getDamager() instanceof LivingEntity attacker
                && event.getEntity() instanceof LivingEntity target
                && MobData.of(attacker).isProcessed()) {
            plugin.traits().onHurtTarget(attacker, target, event);
        }
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    /** FULL_TANK クリーパー：爆発時に近隣エンティティに着火し、地面に火を撒き散らす。 */
    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper) || !MobData.of(creeper).isProcessed()) {
            return;
        }
        Map<Trait, Integer> traits = plugin.traits().read(creeper);
        if (traits.keySet().stream().noneMatch(t -> t.id().equals("full_tank"))) {
            return;
        }
        double r = Math.max(2, creeper.getExplosionRadius());
        for (Entity entity : creeper.getNearbyEntities(r, r, r)) {
            if (entity instanceof LivingEntity living) {
                living.setFireTicks(Math.max(living.getFireTicks(), 100));
            }
        }
        List<Block> blocks = new ArrayList<>(event.blockList());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Block block : blocks) {
                if (ThreadLocalRandom.current().nextDouble() < 0.25
                        && block.getType() == Material.AIR
                        && block.getRelative(BlockFace.DOWN).getType().isSolid()) {
                    block.setType(Material.FIRE);
                }
            }
        });
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (MobData.of(entity).isProcessed()) {
            plugin.traits().onDeath(entity, event);
        }
        plugin.traitDisplay().cleanup(entity);
    }
}
