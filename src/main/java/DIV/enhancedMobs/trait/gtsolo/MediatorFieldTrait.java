package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.trait.base.AuraTrait;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/** 範囲内のプレイヤーにランダムなデバフを付与するオーラ。 */
public final class MediatorFieldTrait extends AuraTrait {

    private static final PotionEffectType[] DEBUFFS = {
            PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.POISON,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS
    };

    public MediatorFieldTrait(int cost, int weight, int maxRank, int minLevel) {
        super("mediator_field", "MEDIATOR", cost, weight, maxRank, minLevel, 6.0, TargetKind.PLAYERS);
    }

    @Override
    protected void affect(LivingEntity mob, int rank, LivingEntity target) {
        PotionEffectType type = DEBUFFS[ThreadLocalRandom.current().nextInt(DEBUFFS.length)];
        target.addPotionEffect(new PotionEffect(type, 40, rank - 1, true, true, true));
    }
}
