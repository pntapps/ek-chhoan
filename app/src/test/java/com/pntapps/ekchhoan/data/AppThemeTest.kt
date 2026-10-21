package com.pntapps.ekchhoan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test
    fun classicIsTheDefault() {
        assertEquals(AppTheme.Classic, AppSettings().theme)
        assertEquals(AppTheme.Classic, AppTheme.parse(null))
        assertEquals(AppTheme.Classic, AppTheme.parse("NoSuchTheme"))
    }

    @Test
    fun earlierLightDarkSystemValuesMapToClassic() {
        assertEquals(AppTheme.Classic, AppTheme.parse("Light"))
        assertEquals(AppTheme.Classic, AppTheme.parse("System"))
        assertEquals(AppTheme.ClassicDark, AppTheme.parse("Dark"))
    }

    @Test
    fun everyThemeRoundTripsByName() {
        AppTheme.entries.forEach { assertEquals(it, AppTheme.parse(it.name)) }
    }
}
