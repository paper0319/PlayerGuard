package net.nekozouneko.playerguard.paid;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionCustomizationRepositoryTest {
    @Test
    void usesSafeDefaultsForExistingRegionsWithoutCustomData() {
        ProtectionCustomizationRepository repository = new ProtectionCustomizationRepository();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("abc", BlockVector3.ZERO, BlockVector3.at(1, 1, 1));

        assertEquals("その他", repository.category(region).label);
    }

    @Test
    void savesCategoryOnRegionFlags() {
        ProtectionCustomizationRepository repository = new ProtectionCustomizationRepository();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("abc", BlockVector3.ZERO, BlockVector3.at(1, 1, 1));

        repository.saveCategory(region, ProtectionCustomizationRepository.Category.HOME);

        assertEquals("自宅", repository.category(region).label);
    }
}
