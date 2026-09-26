package net.nekozouneko.playerguard.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionGuideGUITest {
    @Test
    void keepsGuideInventorySize() {
        assertEquals(54, ProtectionGuideGUI.SIZE);
    }

    @Test
    void groupsGuideTopicsIntoCenteredRows() {
        assertArrayEquals(new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23,
                27, 28, 29, 30, 32, 33, 34, 35, 39}, ProtectionGuideGUI.TOPIC_SLOTS);
        assertEquals(ProtectionGuideTopic.values().length, ProtectionGuideGUI.TOPIC_SLOTS.length);
    }

    @Test
    void keepsPrimaryActionsInTheirRequiredSlots() {
        assertEquals(41, ProtectionGuideGUI.SLOT_GET_AXE);
        assertEquals(53, ProtectionGuideGUI.SLOT_BACK);
    }

    @Test
    void axeGuideUsesRightClickForBothCorners() {
        assertEquals("1点目を右クリック", ProtectionGuideTopic.AXE_USAGE.summary[0]);
        assertEquals("2点目を右クリック", ProtectionGuideTopic.AXE_USAGE.summary[1]);
    }
}
