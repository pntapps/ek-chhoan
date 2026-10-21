package com.pntapps.ekchhoan.ui.home

import com.pntapps.ekchhoan.R
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test fun morningIs5To11() = (5..11).forEach { assertEquals("hour $it", R.string.home_greeting_morning, greetingFor(it)) }

    @Test fun noonIs12To16() = (12..16).forEach { assertEquals("hour $it", R.string.home_greeting_noon, greetingFor(it)) }

    @Test fun eveningIs17To19() = (17..19).forEach { assertEquals("hour $it", R.string.home_greeting_evening, greetingFor(it)) }

    @Test fun nightIsEverythingElse() =
        (listOf(20, 21, 22, 23) + (0..4)).forEach { assertEquals("hour $it", R.string.home_greeting_night, greetingFor(it)) }

    @Test fun greetingsAreDistinct() {
        val all = (0..23).map(::greetingFor).toSet()
        assertEquals(4, all.size)
    }
}
