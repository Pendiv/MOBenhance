package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.TraitRegistry;

/**
 * GTsoloカスタムトレイトを登録する。同一パッケージのためimport不要。
 * 引数は gtcsolo データパック設定の cost/weight/max_rank/min_level。
 */
public final class GTsoloTraits {

    private GTsoloTraits() {
    }

    public static void register(TraitRegistry r) {
        // --- バッチ1: ダメージ/属性イベント系 ---
        r.register(new AngerTrait(20, 75, 3, 100));
        r.register(new CutoffTrait(20, 100, 1, 20));
        r.register(new DivineMightTrait(150, 30, 1, 300));
        r.register(new DistantHorizonTrait(300, 150, 1, 300));
        r.register(new EctothermTrait(50, 35, 1, 100));
        r.register(new IronLegsTrait(10, 300, 1, 100));
        r.register(new FactAdaptationTrait(5, 30, 1, 100));
        r.register(new HungerDrainTrait(50, 80, 4, 100));
        r.register(new FirstMeetingTrait(10, 150, 1, 20));
        r.register(new JustParryTrait(20, 50, 5, 20));
        r.register(new WellFedStrikeTrait(50, 50, 3, 200));
        r.register(new WellFedDefenseTrait(50, 70, 1, 150));
        r.register(new GroupPsychologyTrait(20, 100, 5, 20));

        // --- バッチ1: 初期化/グロウ/バフ系 ---
        r.register(new PreparedTrait(100, 150, 5, 100));
        r.register(new AttentionSeekerTrait(25, 100, 1, 20));
        r.register(new ShowstopperTrait(5, 80, 1, 100));
        r.register(new OtherworldWalkerTrait(5, 240, 3, 100));

        // --- バッチ1: オーラ系 ---
        r.register(new DamageAuraTrait(100, 50, 3, 200));
        r.register(new DeathAuraTrait(400, 30, 5, 500));
        r.register(new FamineTrait(50, 75, 5, 150));

        // --- バッチ1: スケルトン矢系 ---
        r.register(new ExplosiveArrowUserTrait(60, 70, 3, 200));
        r.register(new FloatingArrowTrait(50, 160, 3, 150));
        r.register(new LightningUserTrait(60, 80, 5, 200));

        // --- バッチ1: 時空間系 ---
        r.register(new SpacetimeEntryTrait(50, 100, 1, 300));
        r.register(new SpacetimeLeapTrait(150, 100, 1, 400));

        // --- バッチ2: クリーパー系 ---
        r.register(new ChainDetonationTrait(20, 100, 1, 20));
        r.register(new ExplosiveHeresyTrait(20, 100, 1, 100));
        r.register(new ExplosiveResonanceTrait(20, 100, 1, 100));
        r.register(new FullTankTrait(20, 200, 3, 20));
        r.register(new HairTriggerTrait(20, 150, 3, 100));
        r.register(new ProximityFuseTrait(40, 100, 3, 100));
        r.register(new AccumulationTrait(50, 50, 3, 100));

        // --- バッチ2: 死亡/蘇生系 ---
        r.register(new EndureTrait(15, 50, 1, 50));
        r.register(new IncompleteCombustionTrait(10, 100, 1, 100));
        r.register(new EndlessTaleTrait(50, 100, 10, 100));
        r.register(new SummoningRitualTrait(100, 35, 3, 150));
        r.register(new ResentmentTrait(50, 50, 3, 100));
        r.register(new LastStandTrait(50, 50, 1, 200));
        r.register(new LunaticCurseTrait(200, 25, 1, 200));

        // --- バッチ2: オーラ/時空間/その他 ---
        r.register(new LazinessTrait(75, 25, 3, 200));
        r.register(new SpacetimeRejectionTrait(80, 120, 3, 400));
        r.register(new SpacetimeDiveTrait(150, 100, 1, 400));
        r.register(new SpacetimeRuptureTrait(150, 100, 3, 400));
        r.register(new TossUpTrait(50, 65, 1, 20));
        r.register(new MagicalCreaturesTrait(100, 60, 1, 100));
        r.register(new KinCallTrait(50, 50, 1, 50));
        r.register(new DiurnalTrait(50, 50, 3, 100));
        r.register(new NocturnalTrait(50, 25, 3, 50));

        // --- バッチ3: 即死/時空間系 ---
        r.register(new SoulDestructionTrait(200, 100, 1, 200));
        r.register(new SpacetimeAnnihilationTrait(150, 100, 1, 400));
        r.register(new SpacetimeConformityTrait(80, 120, 1, 400));
        r.register(new SpacetimeDevotionTrait(150, 100, 3, 400));
        r.register(new SpacetimeResonanceTrait(150, 100, 3, 400));
        r.register(new SpacetimeEquilibriumTrait(150, 100, 3, 400));
        r.register(new SpacetimeEternalReturnTrait(150, 100, 1, 400));
        r.register(new SpacetimeTranscendentTrait(200, 40, 1, 400));

        // --- バッチ3: 属性/自己イベント系 ---
        r.register(new DefianceTrait(100, 50, 5, 100));
        r.register(new LoneWolfTrait(50, 100, 5, 100));
        r.register(new HighAltitudeTrait(50, 100, 5, 100));
        r.register(new DominationOverVictoryTrait(50, 50, 4, 200));
        r.register(new KongoYashaTrait(100, 50, 1, 150));
        r.register(new ParadiseLostTrait(100, 25, 1, 150));
        r.register(new CarriedAwayTrait(10, 130, 3, 100));
        r.register(new AudienceEffectTrait(50, 50, 3, 100));
        r.register(new HarmoniousMarchTrait(350, 125, 3, 500));

        // --- バッチ3: スケルトン矢/その他 ---
        r.register(new CrossbowmanTrait(50, 120, 5, 100));
        r.register(new ConsequentialistTrait(40, 60, 3, 100));
        r.register(new ArmorShredderTrait(50, 160, 3, 150));
        r.register(new RebirthTrait(35, 80, 4, 100));
        r.register(new GamblerTrait(50, 50, 1, 20));
        r.register(new InnocenceBattleTrait(30, 100, 1, 20));
        r.register(new WalkingAbyssTrait(5, 100, 1, 20));

        // --- バッチ4: 残り(一部近似実装) ---
        r.register(new AliceSyndromeTrait(20, 120, 1, 100));
        r.register(new AllOrNothingTrait(30, 50, 1, 100));
        r.register(new ArroganceTrait(50, 50, 5, 100));
        r.register(new BomberDispatchTrait(100, 80, 3, 100));
        r.register(new BrokenWindowTrait(150, 50, 1, 250));
        r.register(new BurningPassionTrait(20, 100, 3, 100));
        r.register(new BurstFireTrait(20, 120, 3, 20));
        r.register(new BushidoSpiritTrait(100, 35, 3, 200));
        r.register(new CentripetalForceTrait(50, 150, 3, 200));
        r.register(new ContrarianTrait(20, 100, 1, 20));
        r.register(new CooperativenessTrait(20, 200, 1, 100));
        r.register(new DesperateChargeTrait(150, 75, 3, 250));
        r.register(new DevotionTrait(75, 100, 3, 200));
        r.register(new DistantDeathTrait(400, 50, 3, 500));
        r.register(new DragonicHeartTrait(200, 50, 3, 200));
        r.register(new DreamMeltTrait(200, 50, 3, 200));
        r.register(new EqualTrait(25, 40, 1, 100));
        r.register(new GateTriumphTrait(50, 25, 1, 200));
        r.register(new GroundBattleTrait(20, 100, 1, 200));
        r.register(new HomingShotTrait(20, 100, 1, 20));
        r.register(new JailbreakTrait(100, 80, 3, 300));
        r.register(new LovesickTrait(20, 250, 3, 100));
        r.register(new MagicBulletMarksmanTrait(300, 50, 5, 300));
        r.register(new MediatorFieldTrait(350, 50, 3, 300));
        r.register(new MonotoneCloneTrait(80, 80, 3, 150));
        r.register(new MuscleMemoryTrait(50, 50, 3, 100));
        r.register(new MysticShadowTrait(350, 150, 5, 500));
        r.register(new PandemicTrait(100, 50, 3, 100));
        r.register(new PeerPressureTrait(20, 120, 1, 100));
        r.register(new PhantasmaTrait(150, 70, 3, 300));
        r.register(new PhantomMantleTrait(150, 50, 3, 350));
        r.register(new PureHeartTrait(20, 50, 1, 100));
        r.register(new RapidFireTrait(20, 100, 3, 20));
        r.register(new RejectionUnknownTrait(150, 50, 3, 200));
        r.register(new SecondChanceTrait(120, 40, 1, 200));
        r.register(new SecondSleepTrait(120, 60, 1, 200));
        r.register(new SkyScorchingFlameTrait(200, 50, 3, 250));
        r.register(new StupefactionTrait(250, 50, 1, 250));
        r.register(new SummonKinTrait(120, 175, 4, 200));
        r.register(new TrinityLifeTrait(250, 100, 3, 300));
        r.register(new TrujilloHardinTrait(50, 100, 3, 100));
        r.register(new VainGloryTrait(150, 100, 1, 300));
        r.register(new VolatileMixTrait(20, 200, 5, 20));
        r.register(new WizardryTrait(45, 80, 5, 100));

        // --- バッチ4: 時空間ファミリー ---
        r.register(new SpacetimeBonePickerTrait(80, 120, 5, 400));
        r.register(new SpacetimeChainOfCausalityTrait(200, 40, 3, 400));
        r.register(new SpacetimeConfusionTrait(150, 100, 1, 400));
        r.register(new SpacetimeConquerorTrait(200, 40, 5, 400));
        r.register(new SpacetimeDiffusionTrait(150, 100, 6, 400));
        r.register(new SpacetimeGapTrait(80, 120, 3, 400));
        r.register(new SpacetimeHeroTrait(200, 40, 3, 400));
        r.register(new SpacetimeInfiniteRecursionTrait(200, 40, 2, 400));
        r.register(new SpacetimeShadowTrait(80, 120, 1, 400));
        r.register(new SpacetimeShadowRaidTrait(80, 120, 1, 400));
        r.register(new SpacetimeTidalForceTrait(80, 120, 3, 400));
        r.register(new TurningHeavensTrait(500, 100, 1, 600));
        r.register(new SorrowElegyTrait(500, 100, 3, 600));
    }
}
