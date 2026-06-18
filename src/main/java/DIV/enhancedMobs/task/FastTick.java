package DIV.enhancedMobs.task;

import DIV.enhancedMobs.EnhancedMobs;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1tick周期の高頻度処理レーン（誘導矢・吸引・連射など毎tick前提の特性用）。
 * 登録制のため、未使用時のコストはほぼゼロ。20tick周期の {@link MobTickTask} とは独立。
 *
 * <p>ハンドラが false を返すか例外を投げると登録解除される。登録は（エンティティ×キー）単位
 * （同一キーの再登録は上書き）。キーは特性id等。複数特性が同一モブに併発しても衝突しない。
 * チャンクアンロード等で消えた対象はハンドラ側が false を返して抜けること。
 */
public final class FastTick implements Runnable {

    /** 毎tick呼ばれる処理。継続するなら true、解除するなら false を返す。 */
    public interface Handler {
        boolean tick();
    }

    private static final Map<String, Handler> HANDLERS = new LinkedHashMap<>();

    private static String id(Entity owner, String key) {
        return owner.getUniqueId() + ":" + key;
    }

    public static void register(Entity owner, String key, Handler handler) {
        HANDLERS.put(id(owner, key), handler);
    }

    public static boolean isRegistered(Entity owner, String key) {
        return HANDLERS.containsKey(id(owner, key));
    }

    public static void unregister(Entity owner, String key) {
        HANDLERS.remove(id(owner, key));
    }

    @Override
    public void run() {
        if (HANDLERS.isEmpty()) {
            return;
        }
        // ハンドラの tick() 内から register/unregister が呼ばれてもよいよう、スナップショットを反復する。
        for (Map.Entry<String, Handler> entry : new ArrayList<>(HANDLERS.entrySet())) {
            boolean keep;
            try {
                keep = entry.getValue().tick();
            } catch (Exception e) {
                EnhancedMobs.get().getLogger().warning("FastTick handler error: " + e);
                keep = false;
            }
            if (!keep) {
                // tick() 中に同キーが新ハンドラへ差し替わっていれば消さない（最新を尊重）。
                HANDLERS.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
