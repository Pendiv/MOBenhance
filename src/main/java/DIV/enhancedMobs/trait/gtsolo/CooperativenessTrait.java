package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.MobData;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import DIV.enhancedMobs.trait.TraitService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * 協調性 — 初回 tick に1回だけ、半径16の Mob へ本特性を伝播し、同特性持ちの未マージ個体と
 * グループを組んで ΣHP/ΣMaxHP の共通割合で全員の HP を再配分する
 * （硬い敵は脆く、脆い敵は硬くなる）。以後は独立（常時同期なし）。rank スケールなし。
 */
public final class CooperativenessTrait extends Trait {

    private static final double RADIUS = 16.0;

    public CooperativenessTrait(int cost, int weight, int maxRank, int minLevel) {
        super("cooperativeness", "COOP", cost, weight, maxRank, minLevel);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (EntityState.getInt(mob, "coop_merged", 0) != 0) {
            return;
        }
        // 初回 tick で1度だけ試行（one-shot）。スポーン直後の全回復より後なので再配分が定着する。
        EntityState.setInt(mob, "coop_merged", 1);

        TraitService traits = EnhancedMobs.get().traits();
        List<LivingEntity> group = new ArrayList<>();
        group.add(mob);
        for (Entity entity : mob.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Mob other) || other.isDead() || !MobData.of(other).isProcessed()) {
                continue;
            }
            // 未所持の個体へ伝播（既所持はランク維持 — 原典は昇格しない）。
            if (traits.read(other).keySet().stream().noneMatch(t -> t.id().equals(id()))) {
                traits.addTrait(other, id(), rank);
            }
            if (EntityState.getInt(other, "coop_merged", 0) == 0) {
                group.add(other);
            }
        }

        if (group.size() < 2) {
            return; // 自分だけなら何もしない。
        }

        // 合算 → 共通割合で再配分。
        double totalHp = 0;
        double totalMaxHp = 0;
        for (LivingEntity member : group) {
            totalHp += member.getHealth();
            totalMaxHp += Mobs.maxHealth(member);
        }
        if (totalMaxHp <= 0) {
            return;
        }
        double ratio = totalHp / totalMaxHp;
        for (LivingEntity member : group) {
            member.setHealth(Math.min(Mobs.maxHealth(member), Mobs.maxHealth(member) * ratio));
            EntityState.setInt(member, "coop_merged", 1);
        }
    }
}
