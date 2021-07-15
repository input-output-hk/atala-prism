package io.iohk.atala.prism.app.neo.common.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

suspend fun Bitmap.toEncodedBase64String(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
): String? {
    return withContext(Dispatchers.Default) {
        try {
            val outputStream = ByteArrayOutputStream()
            compress(format, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            return@withContext Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (ex: Exception) {
            return@withContext null
        }
    }
}

suspend fun Bitmap(encodedBase64String: String): Bitmap? {
    return withContext(Dispatchers.Default) {
        try {
            val byteArray = Base64.decode(encodedBase64String, Base64.DEFAULT)
            return@withContext BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            return@withContext null
        }
    }
}

/**
 * Makes a copy of the [Bitmap] object with a specific rotation
 *
 * @param degree [Float] degrees to rotate
 * @return a new instance of [Bitmap]
 */
fun Bitmap.getRotated(degree: Float): Bitmap {
    if (degree == 0f) {
        return copy(config, false)
    }
    val matrix = Matrix()
    matrix.postRotate(degree)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

suspend fun Bitmap.toByteArray(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): ByteArray {
    return withContext(Dispatchers.Default) {
        val stream = ByteArrayOutputStream()
        compress(format, quality, stream)
        return@withContext stream.toByteArray()
    }
}
