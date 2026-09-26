package net.nekozouneko.playerguard;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGConfigTest {

    /** jar 同梱 config.yml 相当の段階制デフォルト（0〜7日）。 */
    private static final String DEFAULTS =
            "protection:\n" +
            "  limit:\n" +
            "    0: 30000\n" +
            "    1: 40000\n" +
            "    2: 50000\n" +
            "    3: 60000\n" +
            "    4: 70000\n" +
            "    5: 80000\n" +
            "    6: 90000\n" +
            "    7: 100000\n";

    private YamlConfiguration load(String yaml) {
        return YamlConfiguration.loadConfiguration(new StringReader(yaml));
    }

    @Test
    void returnsExactTierForEachDay() {
        YamlConfiguration cfg = load(DEFAULTS);
        PGConfig.setConfig(cfg);

        assertEquals(30000, PGConfig.getLimit(0));
        assertEquals(60000, PGConfig.getLimit(3));
        assertEquals(100000, PGConfig.getLimit(7));
    }

    @Test
    void usesNearestLowerTierWhenDayBetweenKeys() {
        // キーが 0/1/2 のみ。7日目は最も近い下位ティア(2)を採用。
        YamlConfiguration cfg = load(
                "protection:\n  limit:\n    0: 30000\n    1: 40000\n    2: 50000\n");
        PGConfig.setConfig(cfg);

        assertEquals(50000, PGConfig.getLimit(7));
    }

    @Test
    void capsAtTopTierBeyondHighestKey() {
        YamlConfiguration cfg = load(DEFAULTS);
        PGConfig.setConfig(cfg);

        // 最上位キー(7)を超える日数は最上位ティアでキャップ。
        assertEquals(100000, PGConfig.getLimit(50));
    }

    @Test
    void appendingDay8TierTakesEffect() {
        // 8日目以降の上限を config に追記すると、追記しただけで効くことを保証。
        YamlConfiguration cfg = load(DEFAULTS + "    8: 110000\n");
        PGConfig.setConfig(cfg);

        assertEquals(110000, PGConfig.getLimit(8));
        assertEquals(110000, PGConfig.getLimit(100), "8日目追記後は最上位が 8 ティアに更新される");
    }

    @Test
    void deniedEntryRelocationDefaultsWhenSectionMissing() {
        YamlConfiguration cfg = load(DEFAULTS);
        PGConfig.setConfig(cfg);

        assertTrue(PGConfig.isDeniedEntryRelocationEnabled());
        assertEquals(8, PGConfig.getDeniedEntryRelocationSearchRadius());
        assertEquals(48, PGConfig.getDeniedEntryRelocationSearchDown());
    }

    @Test
    void deniedEntryRelocationClampsSearchRange() {
        YamlConfiguration cfg = load(
                DEFAULTS +
                "  denied-entry-relocation:\n" +
                "    enabled: false\n" +
                "    search-radius: 99\n" +
                "    search-down: 1\n");
        PGConfig.setConfig(cfg);

        assertFalse(PGConfig.isDeniedEntryRelocationEnabled());
        assertEquals(16, PGConfig.getDeniedEntryRelocationSearchRadius());
        assertEquals(8, PGConfig.getDeniedEntryRelocationSearchDown());
    }

    @Test
    void fallsBackToBundledDefaultWhenSectionMissing() {
        // protection.limit セクションを持たない壊れた config
        // （例: protection_limit_base_value を誤って設定したケース）。
        // getConfig() には jar 同梱デフォルトがセットされている前提を再現する。
        YamlConfiguration cfg = load("protection_limit_base_value: 30000\n");
        cfg.setDefaults(load(DEFAULTS));
        PGConfig.setConfig(cfg);

        assertEquals(30000, PGConfig.getLimit(0),
                "壊れた config でも黙って 0 にならず同梱デフォルトへフォールバックする");
    }
}
