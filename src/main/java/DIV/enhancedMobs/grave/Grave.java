package DIV.enhancedMobs.grave;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1 つの墓の状態。Model B の正本（{@code PENDING} 時はここに中身を保持）。
 * 顕現中（{@code materialized}）はワールドのダブルチェストが中身を持ち、ここは空マーカー。
 *
 * <p>チェストは {@code (x,y,z)} と、その東隣 {@code (x+1,y,z)} の 2 ブロックで構成する
 * （向きは北固定）。アイテムが常にチェスト or {@link #items} の片方のみに存在するよう
 * {@link GraveManager} が状態遷移を管理する。</p>
 */
public final class Grave {

    /** チェストのもう一方（東隣）方向。向きは北固定。 */
    public static final BlockFace SECONDARY = BlockFace.EAST;

    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final String world;
    private int x;
    private int y;
    private int z;
    private final long createdMillis;

    private boolean materialized;
    /** PENDING 時の中身（顕現中は空）。 */
    private final List<ItemStack> items = new ArrayList<>();

    public Grave(UUID id, UUID ownerId, String ownerName, String world,
                 int x, int y, int z, long createdMillis) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdMillis = createdMillis;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public String worldName() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** 顕現位置を変更する（チェストは死亡地点ではなく、近づいた所有者の座標へ出すため）。 */
    public void relocate(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long createdMillis() {
        return createdMillis;
    }

    public boolean isMaterialized() {
        return materialized;
    }

    public void setMaterialized(boolean materialized) {
        this.materialized = materialized;
    }

    public List<ItemStack> items() {
        return items;
    }

    /** プライマリ（左）チェストの座標。ワールド未ロード/欠落なら null。 */
    public Location primaryLocation(World w) {
        return w == null ? null : new Location(w, x, y, z);
    }

    /** セカンダリ（東隣）チェストの座標。 */
    public Location secondaryLocation(World w) {
        return w == null ? null : new Location(w, x + 1, y, z);
    }
}
