package com.pntapps.ekchhoan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_SIDE = 512
private const val JPEG_QUALITY = 85

/** Copies picked family photos into `filesDir/photos`, downscaled and upright. No storage permission needed. */
class PhotoStore(context: Context) {
    private val app = context.applicationContext
    private val dir: File get() = File(app.filesDir, "photos")

    /**
     * Decodes [uri] (EXIF-aware), downscales to at most 512 px and saves it as a JPEG for [memberId].
     * Each import gets a fresh file name so image caches never show a stale photo; the caller deletes
     * the previous file with [delete]. Returns the absolute path, or null when the image can't be read.
     */
    suspend fun importPhoto(uri: Uri, memberId: String): String? = withContext(Dispatchers.IO) {
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(uri) else decodeLegacy(uri)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: OutOfMemoryError) {
            null
        } ?: return@withContext null

        try {
            val folder = dir.apply { mkdirs() }
            val target = File(folder, "${memberId}_${System.currentTimeMillis()}.jpg")
            val tmp = File(folder, target.name + ".tmp")
            val ok = FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (!ok || !tmp.renameTo(target)) {
                tmp.delete()
                return@withContext null
            }
            target.absolutePath
        } catch (e: IOException) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Maps a stored photo path to its file in this install's photos folder by file name, so photos still load
     * after a backup restore, device transfer or on a secondary user where the absolute `filesDir` differs.
     */
    fun resolve(path: String?): File? {
        if (path.isNullOrBlank()) return null
        return File(dir, File(path).name)
    }

    /** Deletes a photo previously returned by [importPhoto]. Ignores null and paths outside the photos folder. */
    fun delete(path: String?) {
        resolve(path)?.delete()
    }

    /** ImageDecoder applies EXIF rotation itself and can decode straight to the target size. */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(app.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val scale = min(1f, MAX_SIDE.toFloat() / max(w, h))
            if (scale < 1f) {
                decoder.setTargetSize(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
            }
        }
    }

    /** API 26–27: sampled BitmapFactory decode, then EXIF rotation and final scale in one pass. */
    private fun decodeLegacy(uri: Uri): Bitmap? {
        val resolver = app.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val degrees = try {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: IOException) {
            0f
        }

        val scale = min(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
        if (degrees == 0f && scale >= 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }
}
