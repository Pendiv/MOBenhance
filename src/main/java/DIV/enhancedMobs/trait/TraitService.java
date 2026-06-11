package DIV.enhancedMobs.trait;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.config.DimensionConfig;
import DIV.enhancedMobs.config.EntityConfig;
import DIV.enhancedMobs.config.MainConfig;
import DIV.enhancedMobs.core.MobData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

/** Central entry point for trait generation, application, and event dispatch. */
public final class TraitService {

    private final TraitRegistry registry;
    private final TraitGenerator generator;
    private final TraitLang lang;

    public TraitService(EnhancedMobs plugin, MainConfig config, EntityConfig entityConfig,
                        DimensionConfig dimensions) {
        this.registry = new TraitRegistry();
        this.registry.registerDefaults();
        this.generator = new TraitGenerator(registry, config, entityConfig, dimensions);
        this.lang = new TraitLang(plugin);
    }

    public TraitRegistry registry() {
        return registry;
    }

    /** Roll traits for a freshly-levelled mob, store them, and apply their initial effects. */
    public Map<Trait, Integer> generateAndApply(LivingEntity mob, int level) {
        Map<Trait, Integer> traits = generator.generate(mob, level);
        MobData.of(mob).setTraitsRaw(registry.serialize(traits));
        traits.forEach((trait, rank) -> trait.initialize(mob, rank));

        // Traits like TANK raise max health; top the mob back up.
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            mob.setHealth(maxHealth.getValue());
        }
        return traits;
    }

    public Map<Trait, Integer> read(LivingEntity mob) {
        return registry.parse(MobData.of(mob).getTraitsRaw());
    }

    public void onHurtTarget(LivingEntity mob, LivingEntity target, EntityDamageByEntityEvent event) {
        read(mob).forEach((trait, rank) -> trait.onHurtTarget(mob, rank, target, event));
    }

    public void onAttacked(LivingEntity mob, EntityDamageEvent event) {
        read(mob).forEach((trait, rank) -> trait.onAttacked(mob, rank, event));
    }

    public void onAttackedBy(LivingEntity mob, LivingEntity attacker, EntityDamageByEntityEvent event) {
        read(mob).forEach((trait, rank) -> trait.onAttackedBy(mob, rank, attacker, event));
    }

    public void tick(LivingEntity mob) {
        read(mob).forEach((trait, rank) -> trait.tick(mob, rank));
    }

    public void onDeath(LivingEntity mob, EntityDeathEvent event) {
        read(mob).forEach((trait, rank) -> trait.onDeath(mob, rank, event));
    }

    /** Japanese label like "頑強2 猛毒 灼熱" for the head display. */
    public String displayText(Map<Trait, Integer> traits) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Trait, Integer> entry : traits.entrySet()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lang.name(entry.getKey()));
            if (entry.getValue() > 1) {
                sb.append(entry.getValue());
            }
        }
        return sb.toString();
    }
}
