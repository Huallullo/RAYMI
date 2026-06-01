package com.raymi.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageOptimizer @Inject constructor(
    private val context: Context
) {
    /**
     * Comprime y redimensiona una imagen desde una URI.
     * Retorna un ByteArray listo para subir a Firebase Storage.
     * Optimizado para reducir costos de almacenamiento y mejorar velocidad de carga.
     */
    fun optimizeImage(uri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 80): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var scale = 1
            while (options.outWidth / scale / 2 >= maxWidth && options.outHeight / scale / 2 >= maxHeight) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            
            val finalInputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(finalInputStream, null, decodeOptions) ?: return null
            finalInputStream.close()

            // Corregir rotación (Crucial para fotos de cámara)
            bitmap = rotateImageIfRequired(bitmap, uri)

            // Redimensionado final exacto
            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val ratio = Math.min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                val width = (bitmap.width * ratio).toInt()
                val height = (bitmap.height * ratio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            }

            val outputStream = ByteArrayOutputStream()
            // Usamos WebP para una compresión superior a JPEG (Soportado desde Android 4.0+)
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
            
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            
            AppLogger.d("ImageOptimizer", "Imagen optimizada: ${byteArray.size / 1024} KB")
            byteArray
        } catch (e: Exception) {
            AppLogger.e("ImageOptimizer", "Error optimizando imagen: ${e.message}")
            null
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        return try {
            val input: InputStream = context.contentResolver.openInputStream(selectedImage) ?: return img
            val ei = ExifInterface(input)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            input.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
                else -> img
            }
        } catch (e: Exception) {
            img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotatedImg != img) {
            img.recycle()
        }
        return rotatedImg
    }
}
