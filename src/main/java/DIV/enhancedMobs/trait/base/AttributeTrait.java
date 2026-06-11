package DIV.enhancedMobs.trait.base;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.List;

/**
 * Trait that applies one or more attribute modifiers scaling with rank (TANK, SPEEDY).
 * Mirrors L2Hostility's {@code AttributeTrait}.
 */
public class AttributeTrait extends Trait {

    public record Entry(Attribute attribute, double perRank, AttributeModifier.Operation operation) {
    }

    private final List<Entry> entries;
    private final NamespacedKey[] keys;

    public AttributeTrait(String id, String shortName, int cost, int weight, int maxRank, int minLevel,
                          List<Entry> entries) {
        super(id, shortName, cost, weight, maxRank, minLevel);
        this.entries = entries;
        this.keys = new NamespacedKey[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            this.keys[i] = new NamespacedKey(EnhancedMobs.get(), "trait_" + id + "_" + i);
        }
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            AttributeInstance inst = mob.getAttribute(entry.attribute());
            if (inst == null) {
                continue;
            }
            NamespacedKey key = keys[i];
            inst.getModifiers().stream()
                    .filter(m -> key.equals(m.getKey()))
                    .toList()
                    .forEach(inst::removeModifier);
            inst.addModifier(new AttributeModifier(key, entry.perRank() * rank, entry.operation()));
        }
    }
}
