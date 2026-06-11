package DIV.enhancedMobs;

import DIV.enhancedMobs.command.CgCommand;
import DIV.enhancedMobs.command.DebugCommand;
import DIV.enhancedMobs.command.DifficultyCommand;
import DIV.enhancedMobs.config.DimensionConfig;
import DIV.enhancedMobs.config.EntityConfig;
import DIV.enhancedMobs.config.LocationConfig;
import DIV.enhancedMobs.config.MainConfig;
import DIV.enhancedMobs.debug.DebugViewers;
import DIV.enhancedMobs.display.TraitDisplay;
import DIV.enhancedMobs.level.DifficultyCalculator;
import DIV.enhancedMobs.level.LevelScaler;
import DIV.enhancedMobs.listener.DebugListener;
import DIV.enhancedMobs.listener.DisplayListener;
import DIV.enhancedMobs.listener.AnvilListener;
import DIV.enhancedMobs.listener.HealListener;
import DIV.enhancedMobs.listener.ItemXpListener;
import DIV.enhancedMobs.listener.MobListener;
import DIV.enhancedMobs.listener.PlayerListener;
import DIV.enhancedMobs.listener.ProjectileListener;
import DIV.enhancedMobs.listener.ResurrectListener;
import DIV.enhancedMobs.listener.SealListener;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.PlayerData;
import DIV.enhancedMobs.task.MobTickTask;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitService;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class EnhancedMobs extends JavaPlugin {

    private static EnhancedMobs instance;

    private MainConfig mainConfig;
    private DifficultyCalculator difficultyCalculator;
    private LevelScaler levelScaler;
    private TraitDisplay traitDisplay;
    private TraitService traitService;
    private DimensionConfig dimensionConfig;

    /** Global accessor so the small helper classes can build NamespacedKeys. */
    public static EnhancedMobs get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.mainConfig = new MainConfig(this);
        this.dimensionConfig = new DimensionConfig(this);
        this.difficultyCalculator = new DifficultyCalculator(mainConfig, dimensionConfig, new LocationConfig(this));
        this.levelScaler = new LevelScaler(this, mainConfig);
        this.traitDisplay = new TraitDisplay(this, mainConfig);
        this.traitService = new TraitService(this, mainConfig, new EntityConfig(this), dimensionConfig);

        // A previous run may have left floating displays if the server crashed.
        traitDisplay.sweepOrphans();

        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new SealListener(), this);
        getServer().getPluginManager().registerEvents(new HealListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new ResurrectListener(), this);
        getServer().getPluginManager().registerEvents(new DisplayListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemXpListener(), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);

        DifficultyCommand difficultyCommand = new DifficultyCommand(this);
        PluginCommand command = getCommand("difficultyset");
        if (command != null) {
            command.setExecutor(difficultyCommand);
            command.setTabCompleter(difficultyCommand);
        }

        // Debug scaffolding (remove before final build).
        DebugViewers debugViewers = new DebugViewers();
        getServer().getPluginManager().registerEvents(new DebugListener(debugViewers), this);
        PluginCommand debugCommand = getCommand("emdebug");
        if (debugCommand != null) {
            debugCommand.setExecutor(new DebugCommand(debugViewers));
        }

        CgCommand cgCommand = new CgCommand();
        PluginCommand cg = getCommand("cg");
        if (cg != null) {
            cg.setExecutor(cgCommand);
            cg.setTabCompleter(cgCommand);
        }

        int tickInterval = Math.max(1, mainConfig.traitTickInterval);
        getServer().getScheduler().runTaskTimer(this, new MobTickTask(this), tickInterval, tickInterval);

        getLogger().info("EnhancedMobs enabled with " + traitService.registry().all().size() + " traits.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (traitDisplay != null) {
            traitDisplay.removeAll();
        }
        getLogger().info("EnhancedMobs disabled.");
    }

    /**
     * Full per-mob setup: store level, scale stats, roll + apply traits, glow, and show the head
     * display. Used by natural spawns and by traits that spawn mobs (e.g. SPLIT).
     */
    public void initializeMob(LivingEntity entity, int level) {
        MobData.of(entity).setLevel(level);
        levelScaler.apply(entity, level);

        Map<Trait, Integer> traits = traitService.generateAndApply(entity, level);
        String traitText = traitService.displayText(traits);

        if (mainConfig.logTraitedSpawns && !traits.isEmpty()) {
            getLogger().info(entity.getType().getKey() + " Lv" + level + " [" + traitText + "]");
        }

        if (mainConfig.glowEnabled && level >= mainConfig.glowStrongLevel) {
            entity.setGlowing(true);
        }

        getServer().getScheduler().runTask(this, () -> {
            if (entity.isValid()) {
                traitDisplay.attach(entity, level, traitText);
            }
        });
    }

    public MainConfig mainConfig() {
        return mainConfig;
    }

    public DifficultyCalculator difficulty() {
        return difficultyCalculator;
    }

    public LevelScaler levelScaler() {
        return levelScaler;
    }

    public TraitDisplay traitDisplay() {
        return traitDisplay;
    }

    public TraitService traits() {
        return traitService;
    }

    public DimensionConfig dimensions() {
        return dimensionConfig;
    }
}
