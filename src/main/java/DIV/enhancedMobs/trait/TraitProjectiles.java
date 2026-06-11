package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;

/** Tags a projectile with the trait that fired it, so ProjectileListener can dispatch its impact. */
public final class TraitProjectiles {

    public static final NamespacedKey TRAIT = new NamespacedKey(EnhancedMobs.get(), "proj_trait");
    public static final NamespacedKey RANK = new NamespacedKey(EnhancedMobs.get(), "proj_rank");

    private TraitProjectiles() {
    }

    public static void tag(Projectile projectile, String traitId, int rank) {
        projectile.getPersistentDataContainer().set(TRAIT, PersistentDataType.STRING, traitId);
        projectile.getPersistentDataContainer().set(RANK, PersistentDataType.INTEGER, rank);
    }
}
