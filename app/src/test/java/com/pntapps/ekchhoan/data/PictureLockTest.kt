package com.pntapps.ekchhoan.data

import com.pntapps.ekchhoan.data.PictureLock.Picture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureLockTest {

    private val seq = listOf(Picture.Cow, Picture.Sun, Picture.Star)

    @Test
    fun nineDistinctPicturesWithDistinctColors() {
        assertEquals(9, Picture.entries.size)
        assertEquals(9, Picture.entries.map { it.argb }.toSet().size)
        assertEquals(9, Picture.entries.map { it.label }.toSet().size)
    }

    @Test
    fun saltIs16RandomBytesHex() {
        val a = PictureLock.newSalt()
        val b = PictureLock.newSalt()
        assertEquals(32, a.length)
        assertTrue(a.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals(a, b)
    }

    @Test
    fun hashIsDeterministicSha256Hex() {
        val h = PictureLock.hash(seq, "abc")
        assertEquals(64, h.length)
        assertEquals(h, PictureLock.hash(seq, "abc"))
        // SHA-256("abc0,3,8")
        assertEquals(sha256("abc0,3,8"), h)
    }

    @Test
    fun hashDependsOnSaltAndOrder() {
        val salt = PictureLock.newSalt()
        assertNotEquals(PictureLock.hash(seq, salt), PictureLock.hash(seq, PictureLock.newSalt()))
        assertNotEquals(PictureLock.hash(seq, salt), PictureLock.hash(seq.reversed(), salt))
    }

    @Test
    fun verifyAcceptsOnlyTheRightSequence() {
        val salt = PictureLock.newSalt()
        val stored = PictureLock.hash(seq, salt)
        assertTrue(PictureLock.verify(seq, salt, stored))
        assertTrue(PictureLock.verify(seq, salt, stored.uppercase()))
        assertFalse(PictureLock.verify(listOf(Picture.Cow, Picture.Sun, Picture.Tree), salt, stored))
        assertFalse(PictureLock.verify(seq, salt, null))
        assertFalse(PictureLock.verify(seq, null, stored))
    }

    @Test
    fun repeatedPicturesAreAValidSequence() {
        val salt = PictureLock.newSalt()
        val same = listOf(Picture.Cow, Picture.Cow, Picture.Cow)
        val stored = PictureLock.hash(same, salt)
        assertTrue(PictureLock.verify(same, salt, stored))
        assertFalse(PictureLock.verify(listOf(Picture.Cow, Picture.Cow, Picture.Sun), salt, stored))
    }

    private fun sha256(s: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
