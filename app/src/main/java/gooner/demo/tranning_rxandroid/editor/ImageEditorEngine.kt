package gooner.demo.tranning_rxandroid.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * All the pixel pushing of the editor, exposed as RxJava streams so the UI never blocks:
 * decoding, thumbnail generation, rendering the final picture and saving it to the gallery.
 */
class ImageEditorEngine(context: Context) {

    private val appContext = context.applicationContext

    /** Decodes [uri], honouring the EXIF rotation, downscaled to fit [maxSize] pixels. */
    fun loadBitmap(uri: Uri, maxSize: Int): Single<Bitmap> {
        return Single.fromCallable {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            openStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("Unable to read the picture bounds")
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val decoded = openStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("Unable to decode the picture")

            fitInside(applyExifRotation(uri, decoded), maxSize)
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Emits one preview per [PhotoFilter], in order, as soon as it is ready. The heavy
     * work happens on the computation scheduler while the filter strip fills up.
     */
    fun filterThumbnails(source: Bitmap, thumbnailSize: Int): Observable<FilterThumbnail> {
        return Single.fromCallable { squareThumbnail(source, thumbnailSize) }
            .flatMapObservable { thumbnail ->
                Observable.fromArray(*PhotoFilter.values())
                    .map { filter ->
                        FilterThumbnail(filter, applyColorMatrix(thumbnail, filter.colorMatrix()))
                    }
            }
            .subscribeOn(Schedulers.computation())
    }

    /** Re-draws the photo, its colour filter and all the overlays at full resolution. */
    fun render(snapshot: EditorSnapshot): Single<Bitmap> {
        return Single.fromCallable {
            val source = snapshot.baseBitmap
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(snapshot.colorMatrix)
            canvas.drawBitmap(source, 0f, 0f, paint)
            for (overlay in snapshot.overlays) {
                overlay.draw(canvas)
            }
            output
        }.subscribeOn(Schedulers.computation())
    }

    /** Writes [bitmap] as a JPEG into the shared Pictures collection and returns its uri. */
    fun saveToGallery(bitmap: Bitmap, displayName: String): Single<Uri> {
        return Single.fromCallable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(bitmap, displayName)
            } else {
                saveWithLegacyMediaStore(bitmap, displayName)
            }
        }.subscribeOn(Schedulers.io())
    }

    /** Best effort human readable name for a picked document, used for the sound chip. */
    fun displayNameOf(uri: Uri): Single<String> {
        return Single.fromCallable {
            var name: String? = null
            val cursor = appContext.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        name = it.getString(columnIndex)
                    }
                }
            }
            name ?: uri.lastPathSegment ?: uri.toString()
        }.subscribeOn(Schedulers.io())
    }

    private fun saveWithMediaStore(bitmap: Bitmap, displayName: String): Uri {
        val resolver = appContext.contentResolver
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/" + ALBUM_NAME
        )
        values.put(MediaStore.Images.Media.IS_PENDING, 1)

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("The gallery refused to create the file")
        try {
            val stream: OutputStream = resolver.openOutputStream(uri)
                ?: throw IOException("Unable to open the saved file for writing")
            stream.use {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)) {
                    throw IOException("Unable to encode the picture")
                }
            }
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }

        val done = ContentValues()
        done.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, done, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacyMediaStore(bitmap: Bitmap, displayName: String): Uri {
        val inserted = MediaStore.Images.Media.insertImage(
            appContext.contentResolver, bitmap, displayName, ALBUM_NAME
        ) ?: throw IOException("The gallery refused to create the file")
        return Uri.parse(inserted)
    }

    private fun openStream(uri: Uri): InputStream =
        appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open the selected file")

    private fun sampleSizeFor(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        var largest = Math.max(width, height)
        while (largest / 2 >= maxSize) {
            largest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun fitInside(bitmap: Bitmap, maxSize: Int): Bitmap {
        val largest = Math.max(bitmap.width, bitmap.height)
        if (largest <= maxSize) {
            return bitmap
        }
        val ratio = maxSize.toFloat() / largest
        val width = Math.max(1, Math.round(bitmap.width * ratio))
        val height = Math.max(1, Math.round(bitmap.height * ratio))
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            openStream(uri).use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (error: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun squareThumbnail(source: Bitmap, size: Int): Bitmap {
        val shortest = Math.min(source.width, source.height)
        val left = (source.width - shortest) / 2
        val top = (source.height - shortest) / 2
        val cropped = Bitmap.createBitmap(source, left, top, shortest, shortest)
        val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
        if (scaled != cropped && cropped != source) {
            cropped.recycle()
        }
        return scaled
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        Canvas(output).drawBitmap(source, 0f, 0f, paint)
        return output
    }

    companion object {
        private const val ALBUM_NAME = "PhotoEditor"
        private const val JPEG_QUALITY = 95
    }
}

/** A filter together with the preview shown in the filter strip. */
class FilterThumbnail(val filter: PhotoFilter, val preview: Bitmap)
