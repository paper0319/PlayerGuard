package net.nekozouneko.playerguard.paid;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public final class ProtectionCustomizationRepository {
    public List<Category> categories() {
        return Arrays.asList(Category.values());
    }

    public Category category(ProtectedRegion region) {
        return Category.fromId(region.getFlag(PGCustomFlags.CUSTOM_CATEGORY));
    }

    public void saveCategory(ProtectedRegion region, Category category) {
        region.setFlag(PGCustomFlags.CUSTOM_CATEGORY, category.id);
    }

    public enum Category {
        HOME("home", "自宅", Material.RED_BED),
        SHARE_HOUSE("share_house", "シェアハウス", Material.OAK_DOOR),
        SHOP("shop", "ショップ", Material.EMERALD),
        MARKET("market", "市場", Material.CHEST),
        FARM("farm", "農場", Material.WHEAT),
        RANCH("ranch", "牧場", Material.HAY_BLOCK),
        FACTORY("factory", "工場", Material.FURNACE),
        STORAGE("storage", "倉庫", Material.BARREL),
        MINE("mine", "鉱山", Material.IRON_PICKAXE),
        QUARRY("quarry", "採掘場", Material.STONE_PICKAXE),
        TOWN("town", "街・村", Material.BELL),
        PUBLIC("public", "公共施設", Material.BEACON),
        ROAD("road", "道路", Material.STONE_BRICKS),
        STATION("station", "駅・交通施設", Material.MINECART),
        PORT("port", "港", Material.OAK_BOAT),
        EVENT("event", "イベント会場", Material.FIREWORK_ROCKET),
        ARENA("arena", "アリーナ", Material.IRON_SWORD),
        TRAP("trap", "トラップ施設", Material.SKELETON_SKULL),
        EXP("exp", "経験値施設", Material.EXPERIENCE_BOTTLE),
        RESOURCE("resource", "資源施設", Material.IRON_BLOCK),
        SHOWCASE("showcase", "建築展示", Material.BRICKS),
        TOURISM("tourism", "観光地", Material.SPYGLASS),
        MANAGEMENT("management", "管理施設", Material.COMPARATOR),
        OTHER("other", "その他", Material.PAPER);

        public final String id;
        public final String label;
        public final Material icon;

        Category(String id, String label, Material icon) {
            this.id = id;
            this.label = label;
            this.icon = icon;
        }

        static Category fromId(String id) {
            for (Category value : values()) {
                if (value.id.equalsIgnoreCase(String.valueOf(id))) return value;
            }
            return OTHER;
        }
    }
}
