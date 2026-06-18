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
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.Map;

/** 特性生成・適用・イベントディスパッチの中心窓口。 */
public final class TraitService {

    private final EnhancedMobs plugin;
    private final MainConfig config;
    private final EntityConfig entityConfig;
    private final DimensionConfig dimensions;
    private final TraitRegistry registry;
    private final TraitGenerator generator;
    private final TraitLang lang;

    public TraitService(EnhancedMobs plugin, MainConfig config, EntityConfig entityConfig,
                        DimensionConfig dimensions) {
        this.plugin = plugin;
        this.config = config;
        this.entityConfig = entityConfig;
        this.dimensions = dimensions;
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

    /** レベルが決まったモブの特性を抽選し、PDC に保存して初期化を呼ぶ。 */
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
     * 指定特性をモブの保存セットから除去する。
     * クローン/召喚特性が生成したコピーに同じ特性を引き継がせないため
     */
    public void stripTrait(LivingEntity mob, String id) {
        Map<Trait, Integer> traits = read(mob);
        Trait trait = registry.byId(id);
        if (trait != null && traits.remove(trait) != null) {
            MobData.of(mob).setTraitsRaw(registry.serialize(traits));
            refreshDisplay(mob, traits);
        }
    }

    /**
     * 特性を動的に付与する（伝播・覚醒・憑依系特性用。{@link #stripTrait} の対）。
     * disabled-traits / appliesTo / entityConfig / ランク上限を生成時と同じ基準で尊重する。
     * 既ランクが指定以上なら何もしない。昇格時は {@code initialize} が再実行されるため、
     * 非冪等な initialize を持つ特性に使う場合は注意。
     *
     * @return 付与（または昇格）したら true
     */
    public boolean addTrait(LivingEntity mob, String id, int rank) {
        return addTrait(mob, id, rank, false);
    }

    /**
     * {@link #addTrait(LivingEntity, String, int)} の強制版。
     * {@code force=true} のとき disabled-traits / appliesTo / entityConfig のゲートを無視して付与する
     * （三体生命などが内部モード特性を確実に付与するため。ランク上限は通常どおり尊重）。
     */
    public boolean addTrait(LivingEntity mob, String id, int rank, boolean force) {
        Trait trait = registry.byId(id);
        if (trait == null || rank <= 0) {
            return false;
        }
        if (!force && (dimensions.isTraitDisabled(id)
                || !trait.appliesTo(mob)
                || !entityConfig.allows(mob.getType(), id))) {
            return false;
        }
        Map<Trait, Integer> traits = read(mob);
        int current = traits.getOrDefault(trait, 0);
        int target = Math.min(rank, Math.min(config.traitGlobalMaxRank, trait.maxRank()));
        if (target <= current) {
            return false;
        }
        traits.put(trait, target);
        MobData.of(mob).setTraitsRaw(registry.serialize(traits));
        trait.initialize(mob, target);
        refreshDisplay(mob, traits);
        return true;
    }

    /**
     * 特性セット変更後に頭上表示を貼り直す。
     * initializeMob 側の遅延 attach より後に実行されるよう1tick遅延させる（FIFO順）。
     */
    private void refreshDisplay(LivingEntity mob, Map<Trait, Integer> traits) {
        String text = displayText(traits);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (mob.isValid()) {
                plugin.traitDisplay().attach(mob, MobData.of(mob).getLevel(), text);
            }
        });
    }

    // いずれかの特性がイベントをキャンセルしたら、以降の特性は被ダメ修飾・反撃を行わない
    // （パリィ成立後に別特性が setDamage でダメージを復活させる/反撃を飛ばす事故を防ぐ）。
    public void onHurtTarget(LivingEntity mob, LivingEntity target, EntityDamageByEntityEvent event) {
        read(mob).forEach((trait, rank) -> {
            if (!event.isCancelled()) {
                safe(trait, mob, () -> trait.onHurtTarget(mob, rank, target, event));
            }
        });
    }

    public void onAttacked(LivingEntity mob, EntityDamageEvent event) {
        read(mob).forEach((trait, rank) -> {
            if (!event.isCancelled()) {
                safe(trait, mob, () -> trait.onAttacked(mob, rank, event));
            }
        });
    }

    public void onAttackedBy(LivingEntity mob, LivingEntity attacker, EntityDamageByEntityEvent event) {
        read(mob).forEach((trait, rank) -> {
            if (!event.isCancelled()) {
                safe(trait, mob, () -> trait.onAttackedBy(mob, rank, attacker, event));
            }
        });
    }

    public void tick(LivingEntity mob) {
        read(mob).forEach((trait, rank) -> safe(trait, mob, () -> trait.tick(mob, rank)));
    }

    public void onDeath(LivingEntity mob, EntityDeathEvent event) {
        read(mob).forEach((trait, rank) -> safe(trait, mob, () -> trait.onDeath(mob, rank, event)));
    }

    public void onExplosionPrime(LivingEntity mob, ExplosionPrimeEvent event) {
        read(mob).forEach((trait, rank) -> safe(trait, mob, () -> trait.onExplosionPrime(mob, rank, event)));
    }

    public void onPotionEffect(LivingEntity mob, EntityPotionEffectEvent event) {
        read(mob).forEach((trait, rank) -> safe(trait, mob, () -> trait.onPotionEffect(mob, rank, event)));
    }

    public void onTargeted(LivingEntity mob, EntityTargetLivingEntityEvent event) {
        read(mob).forEach((trait, rank) -> safe(trait, mob, () -> trait.onTargeted(mob, rank, event)));
    }

    /**
     * 個々の特性ハンドラを例外隔離して実行する。1特性が想定外状態（消滅済みエンティティ等）で
     * 投げても、同一モブの他特性や tick タスクの残りモブを巻き添えにしない。
     */
    private void safe(Trait trait, LivingEntity mob, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            plugin.getLogger().warning("Trait '" + trait.id() + "' threw on "
                    + mob.getType() + " (" + mob.getUniqueId() + "): " + e);
        }
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
