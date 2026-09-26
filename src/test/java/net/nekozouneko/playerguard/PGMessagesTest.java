package net.nekozouneko.playerguard;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PGMessagesTest {

    @Test
    void deniedEntryTitleIsTheOnScreenWarning() {
        assertEquals("その土地には入れません", ChatColor.stripColor(PGMessages.deniedEntryTitle()));
        assertEquals("メンバー以外は入場できません", ChatColor.stripColor(PGMessages.deniedEntrySubtitle()));
    }
}
