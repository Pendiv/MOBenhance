package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.item.ItemSkills;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

/** 有効化時に一度だけ読み込む config.yml の値のスナップショット。 */
public final class MainConfig {

    public final boolean levelingEnabled;
    public final int maxMobLevel;
    public final double distanceFactor;
    public final double finalMultiplier;

    public final boolean enhancementEnabled;
    public final double itemXpMultiplier;
    public final int coarseDraws;
    public final double enhHealth;
    public final double enhArmor;
    public final double enhAttack;
    public final double enhSpeed;
    public final double maxSpeedBonus;

    public final boolean traitsEnabled;
    public final int traitMaxCount;
    public final double traitSuppression;
    public final int traitTickInterval;
    public final double traitCostFactor;
    public final int traitGlobalMaxRank;

    public final boolean logTraitedSpawns;
    public final int baseLevel;
    public final double variation;
    public final double netherValue;
    public final double endValue;
    public final double importanceHalfKills;

    public final boolean glowEnabled;
    public final int glowStrongLevel;
    public final boolean headEnabled;
    public final double headViewDistance;

    /** config の skills.* で無効化されたレベリングスキル id。抽選も発動も行われない。 */
    private final Set<String> disabledSkills;

    public MainConfig(EnhancedMobs plugin) {
        FileConfiguration c = plugin.getConfig();
        this.levelingEnabled = c.getBoolean("leveling.enabled", true);
        this.maxMobLevel = c.getInt("leveling.max-mob-level", 500);
        this.distanceFactor = c.getDouble("leveling.distance-factor", 0.02);
        this.finalMultiplier = c.getDouble("leveling.final-multiplier", 0.72);
        this.enhancementEnabled = c.getBoolean("enhancement.enabled", true);
        this.itemXpMultiplier = c.getDouble("enhancement.xp-multiplier", 1.0);
        this.coarseDraws = c.getInt("enhancement.coarse-draws", 10);
        this.enhHealth = c.getDouble("enhancement.per-step.health", 1.0);
        this.enhArmor = c.getDouble("enhancement.per-step.armor", 0.5);
        this.enhAttack = c.getDouble("enhancement.per-step.attack-damage", 0.25);
        this.enhSpeed = c.getDouble("enhancement.per-step.movement-speed", 0.005);
        this.maxSpeedBonus = c.getDouble("enhancement.max-speed-bonus", 0.3);
        this.traitsEnabled = c.getBoolean("traits.enabled", true);
        this.traitMaxCount = c.getInt("traits.max-count", 4);
        this.traitSuppression = c.getDouble("traits.suppression", 0.1);
        this.traitTickInterval = c.getInt("traits.tick-interval", 20);
        this.traitCostFactor = c.getDouble("traits.cost-factor", 1.0);
        this.traitGlobalMaxRank = c.getInt("traits.global-max-rank", 5);
        this.logTraitedSpawns = c.getBoolean("logging.traited-spawns", false);
        this.baseLevel = c.getInt("default-difficulty.base", 1);
        this.variation = c.getDouble("default-difficulty.variation", 2.0);
        this.netherValue = c.getDouble("danger.nether-value", 16.0);
        this.endValue = c.getDouble("danger.end-value", 25.0);
        this.importanceHalfKills = c.getDouble("danger.importance-half-kills", 100.0);
        this.glowEnabled = c.getBoolean("display.glow.enabled", true);
        this.glowStrongLevel = c.getInt("display.glow.strong-level", 100);
        this.headEnabled = c.getBoolean("display.head.enabled", true);
        this.headViewDistance = c.getDouble("display.head.view-distance", 32.0);
        this.disabledSkills = loadDisabledSkills(c);
    }

    /**
     * skills.&lt;id&gt; を読み、無効化されたスキル id の集合を作る。
     * 既定は全て有効。範囲破壊(area_break)のみ既定で無効。
     */
    private static Set<String> loadDisabledSkills(FileConfiguration c) {
        Set<String> disabled = new HashSet<>();
        for (String id : ItemSkills.SKILL_IDS) {
            boolean defaultEnabled = !id.equals(ItemSkills.SKILL_AREA_BREAK);
            if (!c.getBoolean("skills." + id, defaultEnabled)) {
                disabled.add(id);
            }
        }
        return Set.copyOf(disabled);
    }

    /** レベリングスキルが config で有効か（抽選・発動の可否）。 */
    public boolean skillEnabled(String id) {
        return !disabledSkills.contains(id);
    }
}
