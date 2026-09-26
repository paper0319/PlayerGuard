package net.nekozouneko.playerguard.flag;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import net.nekozouneko.commons.lang.collect.Collections3;
import net.nekozouneko.playerguard.PGUtil;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum GuardFlags {

    // --- 1行目: 基本(プレイヤー操作系)フラグ ---
    BREAK("break", "ブロックの破壊", Material.IRON_PICKAXE, 1, null, true, Flags.BLOCK_BREAK),
    PLACE("place", "ブロックの設置", Material.CRAFTING_TABLE, 1, null, true, Flags.BLOCK_PLACE),
    INTERACT("interact", "アイテムの使用、チェストを開く", Material.REDSTONE, 1, true, true, Flags.USE, Flags.INTERACT, Flags.CHEST_ACCESS, Flags.USE_ANVIL),
    PVP("pvp", "PvP (プレイヤー同士のダメージ)", Material.IRON_SWORD, 1, false, false, Flags.PVP),
    ENTITY_DAMAGE("entity-damage", "エンティティへのダメージ", Material.TRIDENT, 1, true, true, Flags.DAMAGE_ANIMALS),
    ENTRY("entry", "メンバー以外の侵入", Material.BARRIER, 1, null, true, Flags.ENTRY, Flags.ENDERPEARL, Flags.CHORUS_TELEPORT),
    PISTONS("pistons", "ピストンの使用", Material.PISTON, 1, true, true, Flags.PISTONS, Flags.USE_DRIPLEAF),
    // --- 2行目: ワールド/環境系フラグ ---
    EXPLOSION("explosion", "爆発によるダメージ", Material.TNT, 2, false, false, Flags.CREEPER_EXPLOSION, Flags.TNT, Flags.OTHER_EXPLOSION),
    FIRE("fire", "火の延焼", Material.FLINT_AND_STEEL, 2, true, false, Flags.FIRE_SPREAD, Flags.LAVA_FIRE),
    MOB_SPAWNING("mob-spawning", "モブのスポーン", Material.ZOMBIE_HEAD, 2, true, false, Flags.MOB_SPAWNING),
    ITEM("item", "アイテムのドロップ・拾う", Material.HOPPER, 2, true, true, Flags.ITEM_DROP, Flags.ITEM_PICKUP),
    ENVIRONMENT("environment", "環境変化（成長・葉の消滅）", Material.OAK_SAPLING, 2, true, false, Flags.LEAF_DECAY, Flags.GRASS_SPREAD, Flags.MUSHROOMS, Flags.VINE_GROWTH, Flags.CROP_GROWTH);

    public enum State {
        ALLOW,DENY,UNSET,SOME_CHANGED
    }

    private final String configId;
    private final String displayName;
    private final Material icon;
    /** GUIで表示する行(1始まり)。 */
    private final int guiRow;
    private final Boolean defaultValue;
    /** true なら非メンバーにのみ適用(RegionGroup.NON_MEMBERS)、false ならグループ無し(全体に適用)。 */
    private final boolean nonMemberScoped;
    private final StateFlag[] flags;

    GuardFlags(String configId, String displayName, Material icon, int guiRow, Boolean defaultValue, boolean nonMemberScoped, StateFlag... flags) {
        this.configId = configId;
        this.displayName = displayName;
        this.icon = icon;
        this.guiRow = guiRow;
        this.defaultValue = defaultValue;
        this.nonMemberScoped = nonMemberScoped;
        this.flags = flags;
    }

    /** このフラグを適用する RegionGroup。グループを付けない場合は null。 */
    public RegionGroup regionGroup() {
        return nonMemberScoped ? RegionGroup.NON_MEMBERS : null;
    }

    public static State getState(ProtectedRegion pr, GuardFlags flag) {
        List<StateFlag.State> states = new ArrayList<>();
        for (StateFlag fl : flag.getFlags()) {
            states.add(pr.getFlag(fl));
        }

        if (Collections3.allValueEquals(states, StateFlag.State.ALLOW)) {
            return State.ALLOW;
        }
        else if (Collections3.allValueEquals(states, StateFlag.State.DENY)) {
            return State.DENY;
        }
        else if (Collections3.allValueEquals(states, null)) {
            return State.UNSET;
        }
        else return State.SOME_CHANGED;
    }

    public static void initRegionFlags(ProtectedRegion region) {
        for (GuardFlags gf : values()) {
            StateFlag.State state = PGUtil.boolToState(gf.getDefaultValue());
            for (StateFlag f : gf.getFlags()) {
                region.setFlag(f, state);
                region.setFlag(f.getRegionGroupFlag(), gf.regionGroup());
            }
        }
    }

    public static void initRegionFlag(ProtectedRegion region, GuardFlags flag) {
        StateFlag.State state = PGUtil.boolToState(flag.getDefaultValue());
        for (StateFlag f : flag.getFlags()) {
            region.setFlag(f, state);
            region.setFlag(f.getRegionGroupFlag(), flag.regionGroup());
        }
    }

}
