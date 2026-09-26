package net.nekozouneko.playerguard.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionCategoryGUITest {
    @Test
    void centersAShortFinalPageInTheFixedCategoryArea() {
        assertArrayEquals(new int[]{12, 13, 14}, ProtectionCategoryGUI.categorySlotsForCount(3));
        assertArrayEquals(new int[]{12, 14}, ProtectionCategoryGUI.categorySlotsForCount(2));
        assertArrayEquals(new int[]{13}, ProtectionCategoryGUI.categorySlotsForCount(1));
    }

    @Test
    void calculatesPageCountUsingTheFixedPageSize() {
        assertEquals(1, ProtectionCategoryGUI.totalPages(1));
        assertEquals(1, ProtectionCategoryGUI.totalPages(ProtectionCategoryGUI.CATEGORY_SLOTS.length));
        assertEquals(2, ProtectionCategoryGUI.totalPages(ProtectionCategoryGUI.CATEGORY_SLOTS.length + 1));
    }
}