package DIV.enhancedMobs;

import DIV.enhancedMobs.command.EmobCommand;
import DIV.enhancedMobs.config.DimensionConfig;
import DIV.enhancedMobs.config.EntityConfig;
import DIV.enhancedMobs.config.LocationConfig;
import DIV.enhancedMobs.config.MainConfig;
import DIV.enhancedMobs.config.MobBonusConfig;
import DIV.enhancedMobs.debug.DebugViewers;
import DIV.enhancedMobs.display.SideboardIntegration;
import DIV.enhancedMobs.display.TraitDisplay;
import DIV.enhancedMobs.level.DifficultyCalculator;
import DIV.enhancedMobs.level.LevelScaler;
import DIV.enhancedMobs.listener.DebugListener;
import DIV.enhancedMobs.listener.DisplayListener;
import DIV.enhancedMobs.listener.AnvilListener;
import DIV.enhancedMobs.item.ItemSkills;
import DIV.enhancedMobs.listener.ArmorSkillListener;
import DIV.enhancedMobs.listener.AutoMaceListener;
import DIV.enhancedMobs.listener.AxeSkillListener;
import DIV.enhancedMobs.listener.FireChargeListener;
import DIV.enhancedMobs.listener.FlyingAxeListener;
import DIV.enhancedMobs.listener.MaceSkillListener;
import DIV.enhancedMobs.listener.GateListener;
import DIV.enhancedMobs.listener.HealListener;
import DIV.enhancedMobs.listener.ItemBreakGuardListener;
import DIV.enhancedMobs.listener.ItemXpListener;
import DIV.enhancedMobs.listener.MiningSkillListener;
import DIV.enhancedMobs.listener.ShieldSkillListener;
import DIV.enhancedMobs.listener.TractionListener;
import DIV.enhancedMobs.listener.MobListener;
import DIV.enhancedMobs.listener.PickaxeSkillListener;
import DIV.enhancedMobs.listener.PlayerListener;
import DIV.enhancedMobs.listener.ProjectileListener;
import DIV.enhancedMobs.listener.ResurrectListener;
import DIV.enhancedMobs.listener.SealListener;
import DIV.enhancedMobs.listener.SpearSkillListener;
import DIV.enhancedMobs.listener.SpearThrowListener;
import DIV.enhancedMobs.listener.SwordSkillListener;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.PlayerData;
import DIV.enhancedMobs.core.TraitConditions;
import DIV.enhancedMobs.task.FastTick;
import DIV.enhancedMobs.task.MobTickTask;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitService;
import DIV.enhancedMobs.trait.gtsolo.MediatorFieldTrait;
import DIV.enhancedMobs.world.AmeijiaGate;
import DIV.enhancedMobs.world.AncientGate;
import DIV.enhancedMobs.world.DimensionInstaller;
import DIV.enhancedMobs.listener.AncientGateListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
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
    private MobBonusConfig mobBonusConfig;

    /** NamespacedKey生成用のグローバルアクセサ。 */
    public static EnhancedMobs get() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        // カスタムディメンション(enhancedmobs:ameijia)のデータパックを <world>/datapacks へ配備。
        // Paper 26.1.2 の worldgen 形式へ移行済み(dimension_type/biome/noise_settings を新形式で再生成)。
        // 初回配備時はレジストリ登録のため1回の再起動が必要。
        DimensionInstaller.install(this);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 表示テキストの一元管理 / 多言語対応（GlobalTranslator へ辞書を登録）。
        DIV.enhancedMobs.i18n.Lang.init(this);

        // attributelib への適用条件の登録（特性 initialize より先に必要）。
        TraitConditions.init(this);

        this.mainConfig = new MainConfig(this);
        this.dimensionConfig = new DimensionConfig(this);
        this.mobBonusConfig = new MobBonusConfig(this);
        this.difficultyCalculator = new DifficultyCalculator(mainConfig, dimensionConfig, new LocationConfig(this));
        this.levelScaler = new LevelScaler(this, mainConfig);
        this.traitDisplay = new TraitDisplay(this, mainConfig);
        this.traitService = new TraitService(this, mainConfig, new EntityConfig(this), dimensionConfig);

        // クラッシュ時に残留したDisplayエンティティを除去。
        traitDisplay.sweepOrphans();

        // attributelib のステータスサイドバーへ統合（危険度行の差し替え + 装備強化ジャンル）。
        SideboardIntegration.init(this);

        // アメイジア行きゲート（END_PORTAL）の登録情報をロード。
        AmeijiaGate.init(this);
        // 古代都市の基幹ゲート（自動建築＋リカバリーコンパス起動）。
        AncientGate.init(this);

        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new GateListener(), this);
        getServer().getPluginManager().registerEvents(new AncientGateListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new SealListener(), this);
        getServer().getPluginManager().registerEvents(new HealListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new ResurrectListener(), this);
        getServer().getPluginManager().registerEvents(new DisplayListener(this), this);
        if (mainConfig.enhancementEnabled) {
            getServer().getPluginManager().registerEvents(new ItemXpListener(), this);
            getServer().getPluginManager().registerEvents(new AnvilListener(), this);
            getServer().getPluginManager().registerEvents(new ItemBreakGuardListener(), this);
            // 破壊寸前の自動撤回スイープ（10秒周期。金床GUI/砥石/合成など独自経路外の修理を救済）。
            ItemBreakGuardListener.startBrokenSweep(this);
            getServer().getPluginManager().registerEvents(new FlyingAxeListener(this), this);
            getServer().getPluginManager().registerEvents(new AutoMaceListener(this), this);
            getServer().getPluginManager().registerEvents(new SwordSkillListener(this), this);
            getServer().getPluginManager().registerEvents(new FireChargeListener(this), this);
            getServer().getPluginManager().registerEvents(new MaceSkillListener(this), this);
            getServer().getPluginManager().registerEvents(new ShieldSkillListener(this), this);
            getServer().getPluginManager().registerEvents(new SpearSkillListener(), this);
            getServer().getPluginManager().registerEvents(new SpearThrowListener(this), this);
            getServer().getPluginManager().registerEvents(new TractionListener(this), this);
            getServer().getPluginManager().registerEvents(new PickaxeSkillListener(this), this);
            getServer().getPluginManager().registerEvents(new AxeSkillListener(), this);
            getServer().getPluginManager().registerEvents(new ArmorSkillListener(this), this);
            getServer().getPluginManager().registerEvents(new MiningSkillListener(), this);
            // 付加スキルの周期効果（暗視: 5秒ごとに10秒付与）
            getServer().getScheduler().runTaskTimer(this, ItemSkills::tickBonusEffects, 100L, 100L);
        }

        // デバッグ用足場。リリース前に削除すること。
        DebugViewers debugViewers = new DebugViewers();
        getServer().getPluginManager().registerEvents(new DebugListener(debugViewers), this);

        // 全コマンドを /emob <サブ> に統括する。
        EmobCommand emob = new EmobCommand(this, debugViewers);
        PluginCommand emobCommand = getCommand("emob");
        if (emobCommand != null) {
            emobCommand.setExecutor(emob);
            emobCommand.setTabCompleter(emob);
        }

        registerRecipes();

        int tickInterval = Math.max(1, mainConfig.traitTickInterval);
        getServer().getScheduler().runTaskTimer(this, new MobTickTask(this), tickInterval, tickInterval);
        // 高頻度特性（誘導・吸引等）用の1tickレーン。登録が無ければ即returnで負荷ゼロ。
        getServer().getScheduler().runTaskTimer(this, new FastTick(), 1, 1);
        // デスホライゾーン: 凋落の道の管理・効果適用タスクを起動。
        DIV.enhancedMobs.trait.gtsolo.DeathHorizonTrait.init(this);

        getLogger().info("EnhancedMobs enabled with " + traitService.registry().all().size() + " traits.");
    }

    /** エリトラ追加レシピのキー（レシピブック解禁にも使う）。 */
    public static NamespacedKey elytraRecipeKey() {
        return new NamespacedKey(instance, "enhanced_elytra");
    }

    /** 追加クラフトレシピを登録する。 */
    private void registerRecipes() {
        // エリトラ: [S D S / W N W / W _ W]（S=シュルカーの殻, D=ダイヤブロック, N=ネザースター, W=羊毛）。
        NamespacedKey elytraKey = elytraRecipeKey();
        getServer().removeRecipe(elytraKey); // リロード時の二重登録回避
        ShapedRecipe elytra = new ShapedRecipe(elytraKey, new ItemStack(Material.ELYTRA));
        elytra.shape("SDS", "WNW", "W W");
        elytra.setIngredient('S', Material.SHULKER_SHELL);
        elytra.setIngredient('D', Material.DIAMOND_BLOCK);
        elytra.setIngredient('N', Material.NETHER_STAR);
        elytra.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.WOOL)); // 任意の色の羊毛
        getServer().addRecipe(elytra);
        // 既にオンラインのプレイヤーにはレシピブックへ即解禁（/reload 対応）。
        getServer().getOnlinePlayers().forEach(p -> p.discoverRecipe(elytraKey));
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        // 媒介野: 残存する罠ブロックを元のブロックへ一括復元（永続グリーフィング防止）。
        MediatorFieldTrait.restoreAll();
        if (traitDisplay != null) {
            traitDisplay.removeAll();
        }
        getLogger().info("EnhancedMobs disabled.");
    }

    /**
     * モブの初期化処理（レベル保存・ステータススケーリング・特性付与・グロー・頭上表示）。
     * 自然スポーンおよびSPLIT等の特性によるスポーン時に呼ばれる。
     */
    public void initializeMob(LivingEntity entity, int level) {
        MobData.of(entity).setLevel(level);
        levelScaler.apply(entity, level);

        Map<Trait, Integer> traits = mainConfig.traitsEnabled
                ? traitService.generateAndApply(entity, level)
                : Map.of();
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

    public MobBonusConfig mobBonus() {
        return mobBonusConfig;
    }
}
