package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** ヒット時に対象の装備からランダムな1スロットを損耗させる（CORROSION / EROSION）。 */
public final class DurabilityTrait extends Trait {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
    };

    private final int damagePerRank;

    public DurabilityTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                           int damagePerRank) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.damagePerRank = damagePerRank;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        EntityEquipment equipment = target.getEquipment();
        if (equipment == null) {
            return;
        }
        List<EquipmentSlot> damageable = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack item = equipment.getItem(slot);
            if (item.getType().getMaxDurability() > 0 && item.getItemMeta() instanceof Damageable) {
                damageable.add(slot);
            }
        }
        if (damageable.isEmpty()) {
            return;
        }
        EquipmentSlot slot = damageable.get(ThreadLocalRandom.current().nextInt(damageable.size()));
        ItemStack item = equipment.getItem(slot);
        Damageable meta = (Damageable) item.getItemMeta();
        int max = item.getType().getMaxDurability();
        meta.setDamage(Math.min(max, meta.getDamage() + damagePerRank * rank));
        item.setItemMeta(meta);
        equipment.setItem(slot, item);
    }
}
