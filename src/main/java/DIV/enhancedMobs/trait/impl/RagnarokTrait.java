package DIV.enhancedMobs.trait.impl;

import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.seal.SealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reinterpreted RAGNAROK: on hit, seals a random inventory item into an edible carrier. The
 * player must eat it to restore the original (eat duration = unseal time).
 */
public final class RagnarokTrait extends Trait {

    private final double sealChance;

    public RagnarokTrait(int cost, int weight, int maxRank, int minLevel, double sealChance) {
        super("ragnarok", "RAGNA", cost, weight, maxRank, minLevel);
        this.sealChance = sealChance;
    }

    @Override
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player player) || event.getFinalDamage() <= 0) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > Math.min(1.0, sealChance * rank)) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir() && !SealUtil.isSealed(item)) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        int slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
        inv.setItem(slot, SealUtil.seal(inv.getItem(slot)));
        player.sendMessage(Component.text("An item was sealed! Eat it to break the seal.", NamedTextColor.LIGHT_PURPLE));
    }
}
