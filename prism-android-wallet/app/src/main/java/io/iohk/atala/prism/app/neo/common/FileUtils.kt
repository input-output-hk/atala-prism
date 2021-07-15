package io.iohk.atala.prism.app.neo.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import io.iohk.atala.prism.app.neo.common.extensions.getRotated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileUtils {
    companion object {

        suspend fun decodeBitmapFromUrl(context: Context, photoUrl: String, maxSize: Int? = null): Bitmap? {
            return decodeBitmapFromFile(context, File(photoUrl), maxSize)
        }

        suspend fun decodeBitmapFromFile(context: Context, photoFile: File, maxSize: Int? = null): Bitmap? {
            return decodeBitmapFromUri(context, photoFile.toUri(), maxSize)
        }

        suspend fun decodeBitmapFromUri(context: Context, photoUri: Uri, maxSize: Int? = null): Bitmap? {
            return withContext(Dispatchers.IO) {
                val photoSizeAndOrientation = obtainPhotoSizeAndOrientation(context, photoUri)
                val originalSize = photoSizeAndOrientation.first
                val orientation = photoSizeAndOrientation.second
                val bitmapFactoryOptions = BitmapFactory.Options()
                bitmapFactoryOptions.inJustDecodeBounds = false
                val contentResolver = context.contentResolver
                maxSize?.let {
                    val largerSide = originalSize.width.coerceAtLeast(originalSize.height)
                    val scale = largerSide / it
                    bitmapFactoryOptions.inSampleSize = scale
                }
                try {
                    return@withContext contentResolver.openInputStream(photoUri)!!.use { inputStream ->
                        val resizedImage = BitmapFactory.decodeStream(inputStream, null, bitmapFactoryOptions)
                        val fixedImage = getFixedImage(resizedImage, orientation)
                        resizedImage?.recycle()
                        fixedImage
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext null
            }
        }

        /**
         * Gets the size and orientation information of an image file
         *
         * @param context [Context]
         * @param photoUri [Uri]
         * @return a [Pair] object the parameter A of this object will be a [Size] object and the parameter B could be any orientation declared in [ExifInterface]
         * @see [ExifInterface.ORIENTATION_FLIP_HORIZONTAL]
         * @see [ExifInterface.ORIENTATION_FLIP_VERTICAL]
         * @see [ExifInterface.ORIENTATION_NORMAL]
         * @see [ExifInterface.ORIENTATION_ROTATE_180]
         * @see [ExifInterface.ORIENTATION_ROTATE_270]
         * @see [ExifInterface.ORIENTATION_ROTATE_90]
         * @see [ExifInterface.ORIENTATION_UNDEFINED]
         */
        suspend fun obtainPhotoSizeAndOrientation(context: Context, photoUri: Uri): Pair<Size, Int> {
            return withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                val contentResolver = context.contentResolver
                var orientation: Int = ExifInterface.ORIENTATION_UNDEFINED
                try {
                    contentResolver.openInputStream(photoUri)!!.use { inputStream ->
                        ExifInterface(inputStream).run {
                            orientation = getAttributeInt(ExifInterface.TAG_ORIENTATION, orientation)
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext Pair(Size(options.outWidth, options.outHeight), orientation)
            }
        }

        private fun getFixedImage(photo: Bitmap?, orientation: Int): Bitmap? {
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> photo?.getRotated(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> photo?.getRotated(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> photo?.getRotated(270f)
                else -> photo?.getRotated(0f)
            }
        }
    }
}
