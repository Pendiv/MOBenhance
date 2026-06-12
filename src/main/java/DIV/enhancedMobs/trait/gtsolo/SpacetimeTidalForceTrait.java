package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.MobTags;
import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.ArrayList;

/**
 * 周囲8mの時空タグ持ちMobにかかった有害エフェクトの残り時間を追加で削り、
 * 実効 (1+rank) 倍速でデバフを経過させる（即時除去ではなく「早送り」）。
 */
public final class SpacetimeTidalForceTrait extends AuraTrait {

    public SpacetimeTidalForceTrait(int cost, int weight, int maxRank, int minLevel) {
        super("spacetime_tidal_force", "STTIDAL", cost, weight, maxRank, minLevel, 8.0, TargetKind.MOBS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        MobTags.add(mob, "spacetime");
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        if (!MobTags.has(target, "spacetime")) {
            return;
        }
        // 原典: 20tickごとに 20×rank tick 分余分に削る。tick間隔コンフィグに比例換算して同レートを維持。
        int extra = rank * Math.max(1, EnhancedMobs.get().mainConfig().traitTickInterval);
        for (PotionEffect effect : new ArrayList<>(target.getActivePotionEffects())) {
            if (effect.getType().getCategory() != PotionEffectTypeCategory.HARMFUL || effect.isInfinite()) {
                continue;
            }
            int newDur = effect.getDuration() - extra;
            target.removePotionEffect(effect.getType());
            if (newDur > 0) {
                target.addPotionEffect(new PotionEffect(effect.getType(), newDur, effect.getAmplifier(),
                        effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
            }
        }
    }
}
