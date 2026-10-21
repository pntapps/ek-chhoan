package com.pntapps.ekchhoan.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Renders an installed app's icon as a full square of [px] pixels, like a launcher does but without the
 * launcher's mask (callers clip it to the tile's own rounded square).
 *
 * Adaptive icons are drawn zoomed to their 66/108 safe zone with their own background, so the logo fills
 * the square. Drawn plainly, an icon with a white background (YouTube, Calendar) loses its edges on a white
 * tile and only its small logo shows, which made it look smaller than the colored chips at every icon size.
 */
fun Drawable.toAppIconBitmap(px: Int): ImageBitmap {
    if (this !is AdaptiveIconDrawable) return toBitmap(px, px).asImageBitmap()
    val bitmap = createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // Layers are 108 units; the safe zone is the central 66. Scale so 66 units span the bitmap.
    val full = px * 108f / 66f
    val inset = ((full - px) / 2f).toInt()
    listOfNotNull(background, foreground).forEach { layer ->
        layer.setBounds(-inset, -inset, px + inset, px + inset)
        layer.draw(canvas)
    }
    return bitmap.asImageBitmap()
}
