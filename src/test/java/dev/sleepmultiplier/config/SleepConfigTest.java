package dev.sleepmultiplier.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SleepConfigTest {
    private static final MessageConfig MESSAGES = new MessageConfig("sleep", "phantom", "ok", "bad");

    @Test
    void targetWorldMatchIsExact() {
        SleepConfig config = config(12000, 23000, -1L);

        assertTrue(config.isTargetWorld("world"));
        assertFalse(config.isTargetWorld("World"));
        assertFalse(config.isTargetWorld("world_nether"));
    }

    @Test
    void nightBoundsAreInclusive() {
        SleepConfig config = config(12000, 23000, -1L);

        assertFalse(config.isNight(11999));
        assertTrue(config.isNight(12000));
        assertTrue(config.isNight(18000));
        assertTrue(config.isNight(23000));
        assertFalse(config.isNight(23001));
    }

    @Test
    void phantomResetIsDisabledOnlyForNegativeThreshold() {
        assertTrue(config(12000, 23000, -1L).phantomResetDisabled());
        assertFalse(config(12000, 23000, 0L).phantomResetDisabled());
        assertFalse(config(12000, 23000, 1L).phantomResetDisabled());
    }

    private static SleepConfig config(int start, int end, long phantomThreshold) {
        return new SleepConfig("world", start, end, 1000, 500, phantomThreshold, MESSAGES);
    }
}
