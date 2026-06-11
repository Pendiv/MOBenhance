package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.trait.base.AttributeTrait;
import DIV.enhancedMobs.trait.base.SelfEffectTrait;
import DIV.enhancedMobs.trait.base.TargetEffectTrait;
import DIV.enhancedMobs.trait.gtsolo.GTsoloTraits;
import DIV.enhancedMobs.trait.impl.AdaptiveTrait;
import DIV.enhancedMobs.trait.impl.CursedTrait;
import DIV.enhancedMobs.trait.impl.DementorTrait;
import DIV.enhancedMobs.trait.impl.DispellTrait;
import DIV.enhancedMobs.trait.impl.DrainTrait;
import DIV.enhancedMobs.trait.impl.DurabilityTrait;
import DIV.enhancedMobs.trait.impl.EnderTrait;
import DIV.enhancedMobs.trait.impl.FieryTrait;
import DIV.enhancedMobs.trait.impl.FreezingTrait;
import DIV.enhancedMobs.trait.impl.GrenadeTrait;
import DIV.enhancedMobs.trait.impl.GrowthTrait;
import DIV.enhancedMobs.trait.impl.KillerAuraTrait;
import DIV.enhancedMobs.trait.impl.PullPushTrait;
import DIV.enhancedMobs.trait.impl.RagnarokTrait;
import DIV.enhancedMobs.trait.impl.ReflectTrait;
import DIV.enhancedMobs.trait.impl.ReprintTrait;
import DIV.enhancedMobs.trait.impl.ShulkerTrait;
import DIV.enhancedMobs.trait.impl.SoulBurnerTrait;
import DIV.enhancedMobs.trait.impl.SplitTrait;
import DIV.enhancedMobs.trait.impl.StrikeTrait;
import DIV.enhancedMobs.trait.impl.UndyingTrait;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds all registered traits and (de)serializes the "id:rank;..." PDC form. */
public final class TraitRegistry {

    private final Map<String, Trait> byId = new LinkedHashMap<>();

    public void registerDefaults() {
        // --- Attribute traits ---
        register(new AttributeTrait("tank", "TANK", 20, 100, 5, 20, List.of(
                new AttributeTrait.Entry(Attribute.MAX_HEALTH, 0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                new AttributeTrait.Entry(Attribute.ARMOR, 4.0, AttributeModifier.Operation.ADD_NUMBER),
                new AttributeTrait.Entry(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER))));
        register(new AttributeTrait("speedy", "SPEEDY", 20, 100, 5, 50, List.of(
                new AttributeTrait.Entry(Attribute.MOVEMENT_SPEED, 0.1, AttributeModifier.Operation.MULTIPLY_SCALAR_1))));

        // --- Self-effect traits ---
        register(new SelfEffectTrait("protection", "PROT", 40, 100, 4, 50, PotionEffectType.RESISTANCE, true));
        register(new SelfEffectTrait("invisible", "INVIS", 30, 100, 1, 50, PotionEffectType.INVISIBILITY, false));
        register(new SelfEffectTrait("regen", "REGEN", 30, 100, 5, 50, PotionEffectType.REGENERATION, true));

        // --- Target-effect traits (applied to whatever the mob hits) ---
        register(new TargetEffectTrait("poison", "POISON", 20, 75, 3, 20, PotionEffectType.POISON, 100, true, false));
        register(new TargetEffectTrait("wither", "WITHER", 20, 50, 3, 20, PotionEffectType.WITHER, 80, true, false));
        register(new TargetEffectTrait("slowness", "SLOW", 20, 50, 5, 20, PotionEffectType.SLOWNESS, 60, false, true));
        register(new TargetEffectTrait("weakness", "WEAK", 30, 50, 5, 40, PotionEffectType.WEAKNESS, 80, false, true));
        register(new TargetEffectTrait("blind", "BLIND", 30, 25, 3, 40, PotionEffectType.BLINDNESS, 60, true, false));
        register(new TargetEffectTrait("confusion", "CONF", 30, 25, 3, 40, PotionEffectType.NAUSEA, 100, true, false));
        register(new TargetEffectTrait("levitation", "LEVI", 50, 50, 3, 50, PotionEffectType.LEVITATION, 40, true, false));

        // --- Event traits ---
        register(new FieryTrait(20, 100, 1, 20, 4));

        // --- Tier 1 (force-implemented) ---
        register(new ReflectTrait(80, 50, 5, 100, 0.2));
        register(new StrikeTrait(50, 100, 1, 60, 6.0));
        register(new EnderTrait(120, 100, 1, 150, 8.0));
        register(new UndyingTrait(150, 100, 1, 150));
        register(new SplitTrait(70, 100, 3, 120, 2));
        register(new DurabilityTrait("corrosion", "CORR", 120, 50, 3, 200, 50));
        register(new DurabilityTrait("erosion", "EROS", 120, 50, 3, 200, 100));
        register(new FreezingTrait(30, 50, 3, 50, 60));
        register(new ShulkerTrait(50, 100, 1, 70));
        register(new KillerAuraTrait(100, 50, 3, 300, 6.0, 2.0));
        register(new PullPushTrait("pulling", "PULL", 80, 50, 1, 100, 8.0, 0.4, 1.0));
        register(new PullPushTrait("repelling", "REPEL", 80, 50, 1, 100, 8.0, 0.4, -1.0));

        // --- Tier 3 (reinterpreted; MASTER rejected) ---
        register(new DispellTrait(100, 50, 3, 150, 0.5));
        register(new ReprintTrait(100, 100, 1, 100, 0.5));
        register(new RagnarokTrait(300, 100, 3, 600, 0.2));

        // --- GTsolo custom traits ---
        GTsoloTraits.register(this);

        // --- Tier 2 (compromises; GRAVITY/MOONWALK/ARENA rejected) ---
        register(new CursedTrait(20, 100, 3, 20, 0.4, 100));
        register(new SoulBurnerTrait(50, 50, 3, 70, 3, 60));
        register(new AdaptiveTrait(80, 50, 5, 100, 0.1, 0.8));
        register(new DementorTrait(120, 50, 1, 150, 0.3, 3.0));
        register(new GrenadeTrait(100, 100, 5, 100, 40, 1.5f));
        register(new DrainTrait(80, 100, 3, 100));
        register(new GrowthTrait(60, 300, 3, 100));
    }

    public void register(Trait trait) {
        byId.put(trait.id(), trait);
    }

    public Trait byId(String id) {
        return byId.get(id);
    }

    public Collection<Trait> all() {
        return byId.values();
    }

    public Map<Trait, Integer> parse(String raw) {
        Map<Trait, Integer> map = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String part : raw.split(";")) {
            int sep = part.indexOf(':');
            if (sep < 0) {
                continue;
            }
            Trait trait = byId.get(part.substring(0, sep));
            if (trait == null) {
                continue;
            }
            try {
                map.put(trait, Integer.parseInt(part.substring(sep + 1)));
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }
        return map;
    }

    public String serialize(Map<Trait, Integer> traits) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Trait, Integer> entry : traits.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(entry.getKey().id()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }
}
