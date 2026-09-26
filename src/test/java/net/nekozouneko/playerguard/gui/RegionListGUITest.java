package net.nekozouneko.playerguard.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RegionListGUITest {
    @Test
    void keepsExistingInventorySize() {
        assertEquals(54, RegionListGUI.SIZE);
    }
}