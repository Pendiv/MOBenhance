package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.trait.Trait;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 高速射撃。スケルトン専用。戦闘継続中に弓 AI の射撃間隔が徐々に短縮される。
 *
 * <p>原典はバニラ {@code RangedBowAttackGoal.attackIntervalMin} をリフレクションで実行時書き換えするが、
 * Bukkit から vanilla AI の射撃間隔は直接いじれないため、MobGoals API でバニラの弓ゴールを除去し、
 * 同等ロジック（追跡・視線判定・弓溜め・照準矢）+ 可変 interval の自前ゴールへ置き換える。
 * interval = max(5, round(base × (1 − (0.10 + 0.05N) × min(1, 継続tick/200))))
 * （= 戦闘継続10秒で最大短縮 (10+5N)%・最小 5tick）。base はバニラ既定の難易度依存値
 * （ハード 20 / それ以外 40）。非戦闘（ゴール停止）でランプはリセットされる。
 */
public final class RapidFireTrait extends Trait {

    /** バニラの bowGoal と同じ優先度。 */
    private static final int GOAL_PRIORITY = 4;

    public RapidFireTrait(int cost, int weight, int maxRank, int minLevel) {
        super("rapid_fire", "RAPID", cost, weight, maxRank, minLevel);
    }

    @Override
    public boolean appliesTo(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        install(mob, rank);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        // チャンク再ロードや装備変更（reassessWeaponGoal）でバニラ弓ゴールが復活するため毎回確認する
        install(mob, rank);
    }

    /** バニラ弓ゴールを除去し、自前の可変 interval 弓ゴールを未登録なら登録する。 */
    private static void install(LivingEntity mob, int rank) {
        if (!(mob instanceof AbstractSkeleton skeleton)) {
            return;
        }
        MobGoals goals = Bukkit.getMobGoals();
        goals.removeGoal((Monster) skeleton, VanillaGoal.RANGED_BOW_ATTACK);
        if (!goals.hasGoal(skeleton, RapidBowGoal.key())) {
            goals.addGoal(skeleton, GOAL_PRIORITY, new RapidBowGoal(skeleton, rank));
        }
    }

    /** バニラ RangedBowAttackGoal の移植（ストレイフ・後退は Bukkit API 外のため省略）+ 可変射撃間隔。 */
    private static final class RapidBowGoal implements Goal<AbstractSkeleton> {

        private static final int MIN_INTERVAL = 5;
        private static final int RAMP_TICKS = 200;
        /** バニラの弓溜め時間（この tick 数でフルチャージ射撃）。 */
        private static final int DRAW_TICKS = 20;
        /** バニラ bowGoal の射程 15 ブロック。 */
        private static final double ATTACK_RADIUS_SQ = 15.0 * 15.0;

        private final AbstractSkeleton mob;
        private final int rank;
        private int attackTime = -1;
        private int seeTime;
        /** 戦闘継続 tick（200 でランプ最大）。ゴール停止 = 非戦闘でリセット。 */
        private int sustainedTicks;

        private RapidBowGoal(AbstractSkeleton mob, int rank) {
            this.mob = mob;
            this.rank = rank;
        }

        private static GoalKey<AbstractSkeleton> key() {
            return GoalKey.of(AbstractSkeleton.class, new NamespacedKey(EnhancedMobs.get(), "rapid_fire_bow"));
        }

        @Override
        public boolean shouldActivate() {
            LivingEntity target = mob.getTarget();
            return mob.isValid() && target != null && !target.isDead() && bowHand() != null;
        }

        @Override
        public boolean shouldStayActive() {
            return shouldActivate() || (mob.getPathfinder().hasPath() && bowHand() != null);
        }

        @Override
        public void stop() {
            mob.clearActiveItem();
            mob.getPathfinder().stopPathfinding();
            seeTime = 0;
            attackTime = -1;
            sustainedTicks = 0; // 原典: 非戦闘でランプリセット
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null || target.isDead()) {
                return;
            }
            sustainedTicks++;
            boolean canSee = mob.hasLineOfSight(target);
            if (canSee != (seeTime > 0)) {
                seeTime = 0;
            }
            seeTime += canSee ? 1 : -1;
            // 射程内かつ視認 20tick 以上なら立ち止まって撃ち合い、それ以外は追跡（バニラ準拠）
            if (mob.getLocation().distanceSquared(target.getLocation()) <= ATTACK_RADIUS_SQ && seeTime >= 20) {
                mob.getPathfinder().stopPathfinding();
            } else {
                mob.getPathfinder().moveTo(target, 1.0);
            }
            mob.lookAt(target);
            if (mob.hasActiveItem()) {
                if (!canSee && seeTime < -60) {
                    mob.clearActiveItem(); // 視界を失って3秒 → 溜めを解く（バニラ準拠）
                } else if (canSee && mob.getActiveItemUsedTime() >= DRAW_TICKS) {
                    mob.clearActiveItem();
                    shoot(target);
                    attackTime = currentInterval();
                }
            } else if (--attackTime <= 0 && seeTime >= -60) {
                EquipmentSlot hand = bowHand();
                if (hand != null) {
                    mob.startUsingItem(hand); // 弓を溜め始める
                }
            }
        }

        /** 原典: interval = max(5, round(base × (1 − (0.10+0.05N) × min(1, 継続/200))))。 */
        private int currentInterval() {
            int base = mob.getWorld().getDifficulty() == Difficulty.HARD ? 20 : 40; // バニラ既定値
            double ramp = Math.min(1.0, sustainedTicks / (double) RAMP_TICKS);
            double factor = 1.0 - (0.10 + 0.05 * rank) * ramp;
            return Math.max(MIN_INTERVAL, (int) Math.round(base * factor));
        }

        /** バニラ performRangedAttack 相当: 初速1.6・難易度別ばらつき（イージー10/ノーマル6/ハード2）。 */
        private void shoot(LivingEntity target) {
            double inaccuracy = switch (mob.getWorld().getDifficulty()) {
                case HARD -> 2.0;
                case NORMAL -> 6.0;
                default -> 10.0;
            };
            shootAimedArrow(mob, target, 1.6, inaccuracy);
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f,
                    1.0f / (ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f));
        }

        /** 両手から弓を探す（バニラ isHolding(BOW) 相当）。持っていなければ null。 */
        private EquipmentSlot bowHand() {
            EntityEquipment equipment = mob.getEquipment();
            if (equipment == null) {
                return null;
            }
            if (equipment.getItemInMainHand().getType() == Material.BOW) {
                return EquipmentSlot.HAND;
            }
            if (equipment.getItemInOffHand().getType() == Material.BOW) {
                return EquipmentSlot.OFF_HAND;
            }
            return null;
        }

        @Override
        public GoalKey<AbstractSkeleton> getKey() {
            return key();
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
        }

        /** 原典 SpecialArrow.aimedArrow 相当: 弧補正（水平距離×0.2）+ ばらつき付きの照準矢。 */
        private static Arrow shootAimedArrow(LivingEntity mob, LivingEntity target, double speed,
                                             double inaccuracy) {
            Location eye = mob.getEyeLocation();
            double dx = target.getLocation().getX() - mob.getLocation().getX();
            double dy = target.getLocation().getY() + target.getHeight() / 3.0 - eye.getY();
            double dz = target.getLocation().getZ() - mob.getLocation().getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            Vector dir = new Vector(dx, dy + horiz * 0.2, dz);
            if (dir.lengthSquared() < 1e-6) {
                dir = mob.getLocation().getDirection();
            }
            dir.normalize();
            // vanilla shoot() の三角分布ばらつき近似
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double spread = 0.0172275 * inaccuracy;
            dir.add(new Vector((random.nextDouble() - random.nextDouble()) * spread,
                    (random.nextDouble() - random.nextDouble()) * spread,
                    (random.nextDouble() - random.nextDouble()) * spread));
            Arrow arrow = mob.getWorld().spawn(eye, Arrow.class);
            arrow.setShooter(mob);
            arrow.setVelocity(dir.multiply(speed));
            return arrow;
        }
    }
}
