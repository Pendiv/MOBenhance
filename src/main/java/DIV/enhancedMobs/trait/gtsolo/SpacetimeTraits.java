package DIV.enhancedMobs.trait.gtsolo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 時空特性ファミリー共通の定数・ヘルパ。
 * 原典 {@code ModL2Traits.randomGrantableSpacetime} の「付与可能プール13種」に対応する
 * （超越者の同類化・永劫回帰の復活抽選・拡散の特性伝播などが共用する）。
 */
public final class SpacetimeTraits {

    /** 動的付与が可能な時空特性のidプール（原典13種と同一構成）。 */
    public static final List<String> GRANTABLE = List.of(
            "spacetime_gap", "spacetime_leap", "spacetime_shadow", "spacetime_shadow_raid",
            "spacetime_conformity", "spacetime_rejection", "spacetime_resonance", "spacetime_tidal_force",
            "spacetime_bone_picker", "spacetime_equilibrium", "spacetime_confusion", "spacetime_rupture",
            "spacetime_devotion");

    private SpacetimeTraits() {
    }

    /** プールからランダムに1つの特性idを返す。 */
    public static String randomGrantableId() {
        return GRANTABLE.get(ThreadLocalRandom.current().nextInt(GRANTABLE.size()));
    }
}
