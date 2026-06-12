package DIV.enhancedMobs.trait.gtsolo;

import DIV.enhancedMobs.EnhancedMobs;
import DIV.enhancedMobs.core.EntityState;
import DIV.enhancedMobs.core.Mobs;
import DIV.enhancedMobs.trait.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 人は天を仰ぎ空は廻る — Lv600帯・世界同時1体のみ。
 *
 * <p>不死・ディメンターを内部獲得し、デスポーンしない。所持者が存在する限り、同ワールドの
 * 1024m 以内のプレイヤー（クリエ/スペ除外）は5秒ごとに軽減/回避不能の最大体力5%ダメージを
 * 受け続け、その度に所持者の座標が actionbar に通知される。致死時はトーテムを封じて死亡させる
 * （絶対殺害相当）。チャンク非ロード中も最終座標から効果を継続する。
 *
 * <p>一意性はプラグイン data フォルダの turning_heavens.yml（holder UUID・world・座標）で
 * 永続管理し、後発個体からは特性を剥奪する。所持者の死亡確定で座席を解放する
 * （不死が死をキャンセルする限り onDeath は発火しないため解放されない = 原典と同じ）。
 */
public final class TurningHeavensTrait extends Trait {

    /** ワールド側ダメージループの周期（原典 100t = 5秒）。 */
    private static final long INTERVAL_TICKS = 100L;
    /** 効果範囲。 */
    private static final double RANGE = 1024.0;
    /** 最大体力に対するダメージ率。 */
    private static final double DAMAGE_RATIO = 0.05;

    private final Seat seat;

    public TurningHeavensTrait(int cost, int weight, int maxRank, int minLevel) {
        super("turning_heavens", "HEAVENS", cost, weight, maxRank, minLevel);
        EnhancedMobs plugin = EnhancedMobs.get();
        this.seat = new Seat(new File(plugin.getDataFolder(), "turning_heavens.yml"));
        // ワールド側ループ。特性登録は onEnable 中のため、ここで常設タスクとして開始する
        // （所持者が非ロードでも最終座標から効果を継続させる必要があり、モブ tick には載せない）。
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::worldTick, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void initialize(LivingEntity mob, int rank) {
        EnhancedMobs plugin = EnhancedMobs.get();
        if (seat.holder != null && !seat.isHolder(mob)) {
            // 世界に一体まで: 既存個体が居るので後発からは剥奪する
            plugin.traits().stripTrait(mob, id());
            return;
        }
        seat.claim(mob);
        // 不死＋ディメンターを内部獲得（既所持なら addTrait 側が維持する）
        plugin.traits().addTrait(mob, "undying", 1);
        plugin.traits().addTrait(mob, "dementor", 1);
        // 所持者はデスポーンしない
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
    }

    @Override
    public void tick(LivingEntity mob, int rank) {
        if (seat.holder == null || seat.isHolder(mob)) {
            seat.claim(mob);   // 位置追跡を更新（非ロード時は最終座標が残る）
        } else {
            EnhancedMobs.get().traits().stripTrait(mob, id());   // 後発個体は権利なし
        }
    }

    /** 所持者の死亡が確定したら座席を解放する（不死がキャンセルする間はここに来ない）。 */
    @Override
    public void onDeath(LivingEntity mob, int rank, EntityDeathEvent event) {
        if (seat.isHolder(mob)) {
            seat.release();
        }
    }

    /** 100t ごとのワールド側ダメージループ（所持者非ロード中も最終座標から継続）。 */
    private void worldTick() {
        if (seat.holder == null) {
            return;
        }
        Entity holder = Bukkit.getEntity(seat.holder);
        if (holder instanceof LivingEntity living && living.isValid()) {
            seat.claim(living);   // ロード中は現在座標へ追従
        } else if (holder != null && holder.isDead()) {
            seat.release();       // 取りこぼし対策（通常は onDeath が先に解放する）
            return;
        }
        World world = Bukkit.getWorld(seat.world);
        if (world == null) {
            return;
        }
        Location pos = new Location(world, seat.x, seat.y, seat.z);
        Component notice = Component.text(
                "天が廻っている… 所持者: " + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ(),
                NamedTextColor.DARK_PURPLE);
        for (Player player : world.getPlayers()) {
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(pos) > RANGE * RANGE) {
                continue;
            }
            // 防具・エンチャ・耐性・吸収を全て無視する確定ダメージ（直接 setHealth）
            double damage = Mobs.maxHealth(player) * DAMAGE_RATIO;
            player.setNoDamageTicks(0);
            if (player.getHealth() - damage <= 0) {
                // 絶対殺害: トーテムを封じて死亡させる
                EntityState.setFlag(player, "deny_resurrect", 10);
                player.setHealth(0);
            } else {
                player.setHealth(player.getHealth() - damage);
            }
            player.sendActionBar(notice);
        }
    }

    /** 世界一意トラッカ（原典 TurningHeavensData = SavedData 相当の YAML 永続化）。 */
    private static final class Seat {

        private final File file;
        @Nullable
        private UUID holder;
        private String world;
        private double x, y, z;

        Seat(File file) {
            this.file = file;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            String raw = yml.getString("holder", "");
            if (raw != null && !raw.isEmpty()) {
                try {
                    holder = UUID.fromString(raw);
                } catch (IllegalArgumentException ignored) {
                    // 破損データは空席として扱う
                }
            }
            world = yml.getString("world", "");
            x = yml.getDouble("x");
            y = yml.getDouble("y");
            z = yml.getDouble("z");
        }

        /** 所持者として登録し、ワールド・座標を更新する（変化があった時だけ保存）。 */
        void claim(LivingEntity mob) {
            Location loc = mob.getLocation();
            boolean changed = !mob.getUniqueId().equals(holder)
                    || !mob.getWorld().getName().equals(world)
                    || loc.getBlockX() != (int) Math.floor(x)
                    || loc.getBlockY() != (int) Math.floor(y)
                    || loc.getBlockZ() != (int) Math.floor(z);
            holder = mob.getUniqueId();
            world = mob.getWorld().getName();
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
            if (changed) {
                save();
            }
        }

        void release() {
            holder = null;
            save();
        }

        boolean isHolder(LivingEntity mob) {
            return mob.getUniqueId().equals(holder);
        }

        private void save() {
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("holder", holder == null ? null : holder.toString());
            yml.set("world", world);
            yml.set("x", x);
            yml.set("y", y);
            yml.set("z", z);
            try {
                yml.save(file);
            } catch (IOException e) {
                EnhancedMobs.get().getLogger().warning("turning_heavens.yml の保存に失敗: " + e);
            }
        }
    }
}
