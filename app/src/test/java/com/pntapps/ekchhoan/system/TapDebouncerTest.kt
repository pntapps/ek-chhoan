package com.pntapps.ekchhoan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapDebouncerTest {
    private var now = 10_000L
    private val debouncer = TapDebouncer(windowMs = 800, clock = { now })

    @Test fun firstTapIsAccepted() {
        assertTrue(debouncer.tryTap())
    }

    @Test fun firstTapAtTimeZeroIsAccepted() {
        now = 0L
        assertTrue(debouncer.tryTap())
    }

    @Test fun secondTapWithinWindowIsIgnored() {
        assertTrue(debouncer.tryTap())
        now += 799
        assertFalse(debouncer.tryTap())
    }

    @Test fun tapAtWindowEdgeIsAccepted() {
        assertTrue(debouncer.tryTap())
        now += 800
        assertTrue(debouncer.tryTap())
    }

    @Test fun ignoredTapsDoNotExtendTheWindow() {
        assertTrue(debouncer.tryTap())
        now += 500
        assertFalse(debouncer.tryTap())
        now += 300 // 800 ms after the accepted tap
        assertTrue(debouncer.tryTap())
    }

    @Test fun rapidDoubleTapsOnlyCountOnce() {
        val accepted = (0 until 5).count {
            now += 100
            debouncer.tryTap()
        }
        assertEquals(1, accepted)
    }
}
