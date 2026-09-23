package dev.sleepmultiplier.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerSleepStateTest {
    @Test
    void freshStateIsInactiveAndHasNoAccumulatedSleep() {
        PlayerSleepState state = new PlayerSleepState();

        assertFalse(state.isInBed());
        assertFalse(state.isRecentSleeper());
        assertFalse(state.isPhantomProtectionGranted());
        assertEquals(0L, state.getTotalSleepingNanos(123L));
    }

    @Test
    void startIsIdempotentUntilSleepStops() {
        PlayerSleepState state = new PlayerSleepState();
        state.startSleeping(100L);
        state.startSleeping(150L);

        assertEquals(100L, state.getTotalSleepingNanos(200L));
    }

    @Test
    void completedSessionsAccumulate() {
        PlayerSleepState state = new PlayerSleepState();
        state.startSleeping(10L);
        state.stopSleeping(20L);
        state.startSleeping(30L);
        state.stopSleeping(50L);

        assertEquals(30L, state.getTotalSleepingNanos(100L));
    }

    @Test
    void backwardsClockMovementNeverSubtractsSleep() {
        PlayerSleepState state = new PlayerSleepState();
        state.startSleeping(100L);

        assertEquals(0L, state.getTotalSleepingNanos(90L));
        state.stopSleeping(80L);
        assertEquals(0L, state.getTotalSleepingNanos(200L));
    }

    @Test
    void stopWithoutStartIsHarmless() {
        PlayerSleepState state = new PlayerSleepState();
        state.stopSleeping(500L);
        assertEquals(0L, state.getTotalSleepingNanos(600L));
    }

    @Test
    void stateFlagsRoundTripIndependently() {
        PlayerSleepState state = new PlayerSleepState();
        state.setInBed(true);
        state.setRecentSleeper(true);
        state.setPhantomProtectionGranted(true);

        assertTrue(state.isInBed());
        assertTrue(state.isRecentSleeper());
        assertTrue(state.isPhantomProtectionGranted());

        state.setInBed(false);
        state.setRecentSleeper(false);
        state.setPhantomProtectionGranted(false);

        assertFalse(state.isInBed());
        assertFalse(state.isRecentSleeper());
        assertFalse(state.isPhantomProtectionGranted());
    }
}
