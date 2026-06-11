package DIV.enhancedMobs.config;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.configuration.file.FileConfiguration;

/** Snapshot of config.yml values, read once on enable. */
public final class MainConfig {

    public final int maxMobLevel;
    public final double distanceFactor;
    public final double finalMultiplier;

    public final int coarseDraws;
    public final double enhHealth;
    public final double enhArmor;
    public final double enhAttack;
    public final double enhSpeed;
    public final double maxSpeedBonus;

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

    public MainConfig(EnhancedMobs plugin) {
        FileConfiguration c = plugin.getConfig();
        this.maxMobLevel = c.getInt("leveling.max-mob-level", 500);
        this.distanceFactor = c.getDouble("leveling.distance-factor", 0.02);
        this.finalMultiplier = c.getDouble("leveling.final-multiplier", 0.72);
        this.coarseDraws = c.getInt("enhancement.coarse-draws", 10);
        this.enhHealth = c.getDouble("enhancement.per-step.health", 1.0);
        this.enhArmor = c.getDouble("enhancement.per-step.armor", 0.5);
        this.enhAttack = c.getDouble("enhancement.per-step.attack-damage", 0.25);
        this.enhSpeed = c.getDouble("enhancement.per-step.movement-speed", 0.005);
        this.maxSpeedBonus = c.getDouble("enhancement.max-speed-bonus", 0.3);
        this.traitMaxCount = c.getInt("traits.max-count", 4);
        this.traitSuppression = c.getDouble("traits.suppression", 0.1);
        this.traitTickInterval = c.getInt("traits.tick-interval", 20);
        this.traitCostFactor = c.getDouble("traits.cost-factor", 1.0);
        this.traitGlobalMaxRank = c.getInt("traits.global-max-rank", 5);
        this.logTraitedSpawns = c.getBoolean("logging.traited-spawns", true);
        this.baseLevel = c.getInt("default-difficulty.base", 1);
        this.variation = c.getDouble("default-difficulty.variation", 2.0);
        this.netherValue = c.getDouble("danger.nether-value", 16.0);
        this.endValue = c.getDouble("danger.end-value", 25.0);
        this.importanceHalfKills = c.getDouble("danger.importance-half-kills", 100.0);
        this.glowEnabled = c.getBoolean("display.glow.enabled", true);
        this.glowStrongLevel = c.getInt("display.glow.strong-level", 100);
        this.headEnabled = c.getBoolean("display.head.enabled", true);
        this.headViewDistance = c.getDouble("display.head.view-distance", 32.0);
    }
}
