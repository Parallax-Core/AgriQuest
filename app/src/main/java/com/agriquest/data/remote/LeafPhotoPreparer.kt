package com.agriquest.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.max

/**
 * Turns a camera or gallery photo into a small upright JPEG for the Leaf Detector.
 *
 * The model only looks at 320x320, so the longest side is cut to [MAX_SIDE] px before
 * uploading. That keeps uploads small on mobile data. Phone photos are often stored
 * sideways with an EXIF rotation tag, so the rotation is applied here; the re-encoded
 * JPEG has no EXIF data, and the server would otherwise see a sideways leaf.
 */
object LeafPhotoPreparer {

    const val MAX_SIDE = 512
    private const val JPEG_QUALITY = 90

    /** Reads [source], shrinks and rotates it, saves a copy to [saveTo], and returns the JPEG bytes. */
    @Throws(IOException::class)
    fun prepare(context: Context, source: Uri, saveTo: File): ByteArray {
        val resolver = context.contentResolver

        // With inJustDecodeBounds, decodeStream always returns null and only fills in the size,
        // so only a missing stream means the photo could not be opened.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(source) ?: throw IOException("Could not open the photo.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("That file is not a photo the app can read.")
        }

        // Decode at the smallest power-of-two size that is still at least MAX_SIDE.
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: throw IOException("That file is not a photo the app can read.")

        val orientation = try {
            resolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val jpeg = ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        if (upright !== decoded) decoded.recycle()
        upright.recycle()

        saveTo.parentFile?.mkdirs()
        saveTo.writeBytes(jpeg)
        return jpeg
    }
}
