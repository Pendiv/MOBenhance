package DIV.enhancedMobs.listener;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitProjectiles;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

/** 特性飛翔体の着弾を発射元のトレイトにディスパッチする（RangedTrait 参照）。 */
public final class ProjectileListener implements Listener {

    private final EnhancedMobs plugin;

    public ProjectileListener(EnhancedMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String traitId = projectile.getPersistentDataContainer().get(TraitProjectiles.TRAIT, PersistentDataType.STRING);
        Integer rank = projectile.getPersistentDataContainer().get(TraitProjectiles.RANK, PersistentDataType.INTEGER);
        if (traitId == null || rank == null) {
            return;
        }
        Trait trait = plugin.traits().registry().byId(traitId);
        if (trait == null) {
            return;
        }
        LivingEntity shooter = projectile.getShooter() instanceof LivingEntity living ? living : null;
        trait.onProjectileHit(shooter, rank, projectile, event);
    }
}
