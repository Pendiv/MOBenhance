package DIV.enhancedMobs.trait;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.Nullable;

/**
 * モブ特性基底クラス。シングルトン（1インスタンスを多数のモブに rank 付きで適用）。
 * L2Hostility の {@code MobTrait} に対応する。メタデータフィールドが {@link TraitGenerator}
 * の重み付き抽選を制御し、フックが実際の挙動を実装する。
 */
public abstract class Trait {

    private final String id;
    private final String shortName;
    private final int cost;
    private final int weight;
    private final int maxRank;
    private final int minLevel;

    protected Trait(String id, String shortName, int cost, int weight, int maxRank, int minLevel) {
        this.id = id;
        this.shortName = shortName;
        this.cost = cost;
        this.weight = weight;
        this.maxRank = maxRank;
        this.minLevel = minLevel;
    }

    public final String id() {
        return id;
    }

    /** モブの頭上に表示する省略ラベル（例: "TANK"）。 */
    public final String shortName() {
        return shortName;
    }

    public final int cost() {
        return cost;
    }

    /** コスト係数を掛けた実効コスト（L2H 仕様: {@code max(1, round(cost * factor))}）。 */
    public final int getCost(double factor) {
        return Math.max(1, (int) Math.round(cost * factor));
    }

    public final int weight() {
        return weight;
    }

    public final int maxRank() {
        return maxRank;
    }

    public final int minLevel() {
        return minLevel;
    }

    /** このトレイトを指定モブに抽選できるか（トレイト側の制限）。 */
    public boolean appliesTo(LivingEntity mob) {
        return true;
    }

    /** トレイト付与時に1度だけ呼ばれる（属性値追加・永続エフェクト適用など）。 */
    public void initialize(LivingEntity mob, int rank) {
    }

    /** 定期フック（現時点では未スケジュール。将来のtickベーストレイト向けに予約）。 */
    public void tick(LivingEntity mob, int rank) {
    }

    /** モブが {@code target} にダメージを与えた。 */
    public void onHurtTarget(LivingEntity mob, int rank, LivingEntity target, EntityDamageByEntityEvent event) {
    }

    /** モブがダメージを受けた。 */
    public void onAttacked(LivingEntity mob, int rank, EntityDamageEvent event) {
    }

    /** モブが {@code attacker} からダメージを受けた（生きているソースに解決済み。null の場合あり）。 */
    public void onAttackedBy(LivingEntity mob, int rank, @Nullable LivingEntity attacker,
                             EntityDamageByEntityEvent event) {
    }

    /** モブが死亡した。 */
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
    }

    /** このトレイトが発射した飛翔体（{@link DIV.enhancedMobs.trait.base.RangedTrait} 経由）が着弾した。 */
    public void onProjectileHit(@Nullable LivingEntity shooter, int rank, Projectile projectile,
                                ProjectileHitEvent event) {
    }
}
