package com.pntapps.ekchhoan.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pntapps.ekchhoan.R
import java.security.MessageDigest
import java.security.SecureRandom

/** Picture lock (§4.7): a sequence of 3 pictures, stored only as a salted SHA-256 hash. */
object PictureLock {

    const val SEQUENCE_LENGTH = 3

    /** The 9 pictures. [argb] is the tile background (white icon on top); [label] is for TalkBack. */
    enum class Picture(val argb: Long, @param:StringRes val label: Int) {
        Cow(0xFF6D4C41, R.string.lock_pic_cow),
        Mango(0xFFE08A00, R.string.lock_pic_mango),
        House(0xFF1E5BB8, R.string.lock_pic_house),
        Sun(0xFFC62828, R.string.lock_pic_sun),
        Fish(0xFF00838F, R.string.lock_pic_fish),
        Flower(0xFFAD1457, R.string.lock_pic_flower),
        Boat(0xFF283593, R.string.lock_pic_boat),
        Tree(0xFF1B7F3B, R.string.lock_pic_tree),
        Star(0xFF5B3CC4, R.string.lock_pic_star);

        val color: Color get() = Color(argb)

        /** Computed on demand so JVM unit tests never build vectors. */
        val icon: ImageVector
            get() = when (this) {
                Cow -> Icons.Rounded.Pets
                Mango -> Icons.Rounded.Eco
                House -> Icons.Rounded.Home
                Sun -> Icons.Rounded.WbSunny
                Fish -> Icons.Rounded.SetMeal
                Flower -> Icons.Rounded.LocalFlorist
                Boat -> Icons.Rounded.Sailing
                Tree -> Icons.Rounded.Park
                Star -> Icons.Rounded.Star
            }
    }

    /** 16 random bytes as lowercase hex. */
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    /** Lowercase hex SHA-256 of `salt + "0,4,7"` (picture ordinals). */
    fun hash(sequence: List<Picture>, salt: String): String {
        val input = salt + sequence.joinToString(",") { it.ordinal.toString() }
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).toHex()
    }

    /** Constant-time check of [sequence] against a stored hash. False when nothing is stored. */
    fun verify(sequence: List<Picture>, salt: String?, expectedHash: String?): Boolean {
        if (salt == null || expectedHash == null) return false
        val actual = hash(sequence, salt).toByteArray(Charsets.US_ASCII)
        return MessageDigest.isEqual(actual, expectedHash.lowercase().toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
