package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.Trait;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 攻撃者（プレイヤー・モブ問わず）のメインハンド武器が提供する ATTACK_DAMAGE の
 * 固定加算分だけ受けるダメージを差し引く（下限0）。素手・バフ分は減算しない。
 */
public final class ParadiseLostTrait extends Trait {

    public ParadiseLostTrait(int cost, int weight, int maxRank, int minLevel) {
        super("paradise_lost", "PARADISE", cost, weight, maxRank, minLevel);
    }

    @Override
    public void onAttackedBy(LivingEntity mob, int rank, LivingEntity attacker, EntityDamageByEntityEvent event) {
        if (attacker == null || attacker.getEquipment() == null) {
            return;
        }
        ItemStack weapon = attacker.getEquipment().getItemInMainHand();
        if (weapon.getType().isAir()) {
            return;
        }
        // メタにモディファイアがあればそれを、なければ素材デフォルト（バニラ武器の素の攻撃力）を使う
        ItemMeta meta = weapon.getItemMeta();
        Multimap<Attribute, AttributeModifier> mods = meta != null && meta.hasAttributeModifiers()
                ? meta.getAttributeModifiers(EquipmentSlot.HAND)
                : weapon.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND);
        double weaponAtk = 0;
        for (AttributeModifier mod : mods.get(Attribute.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                weaponAtk += mod.getAmount();
            }
        }
        if (weaponAtk > 0) {
            event.setDamage(Math.max(0, event.getDamage() - weaponAtk));
        }
    }
}
