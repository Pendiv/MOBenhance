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

/** トレイト生成・適用・イベントディスパッチの中心窓口。 */
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

    public TraitLang lang() {
        return lang;
    }

    /** レベルが決まったモブのトレイトを抽選し、PDC に保存して初期化を呼ぶ。 */
    public Map<Trait, Integer> generateAndApply(LivingEntity mob, int level) {
        Map<Trait, Integer> traits = generator.generate(mob, level);
        MobData.of(mob).setTraitsRaw(registry.serialize(traits));
        traits.forEach((trait, rank) -> trait.initialize(mob, rank));

        // TANK などは最大HP を増やすため、初期化後に全回復する。
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            mob.setHealth(maxHealth.getValue());
        }
        return traits;
    }

    public Map<Trait, Integer> read(LivingEntity mob) {
        return registry.parse(MobData.of(mob).getTraitsRaw());
    }

    /**
     * 指定トレイトをモブの保存セットから除去する。
     * クローン/召喚トレイトが生成したコピーに同じトレイトを引き継がせないために使う（無限増殖防止）。
     */
    public void stripTrait(LivingEntity mob, String id) {
        Map<Trait, Integer> traits = read(mob);
        Trait trait = registry.byId(id);
        if (trait != null && traits.remove(trait) != null) {
            MobData.of(mob).setTraitsRaw(registry.serialize(traits));
        }
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

    /** 頭上表示用の日本語ラベル（例: "頑強2 猛毒 灼熱"）。 */
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
