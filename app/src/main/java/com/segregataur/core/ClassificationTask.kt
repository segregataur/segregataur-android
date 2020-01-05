package com.segregataur.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Log
import com.segregataur.db.ClassificationDAO
import com.segregataur.db.ClassificationDataEntity
import com.segregataur.util.ByteBufferPool
import com.segregataur.util.Utils.calculateInSampleSize
import java.nio.ByteBuffer
import java.sql.Date
import java.util.concurrent.Callable

class ClassificationTask(
        private val path: String,
        private val byteBufferPool: ByteBufferPool,
        private val junkImageClassifier: JunkImageClassifier,
        private val classificationDAO: ClassificationDAO) : Callable<Unit> {

    override fun call() {
        val (buffer, id) = byteBufferPool.get()
        try {
            val classificationData = classify(path, buffer, junkImageClassifier)
            classificationDAO.insert(classificationData)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } finally {
            byteBufferPool.free(id)
        }
    }

    private fun classify(path: String, byteBufferForImageData: ByteBuffer,
                         junkImageClassifier: JunkImageClassifier)
            : ClassificationDataEntity {

        val bitmapToBeClassified = try {
            loadSampledBitmap(path, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
        } catch (e: IllegalStateException) {
            val videoThumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)
            Bitmap.createScaledBitmap(videoThumbnail, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, false)
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bitmapToBeClassified, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, false)
        val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
        copyBitmapIntoByteBuffer(byteBufferForImageData, scaledBitmap, intValues)
        return ClassificationDataEntity(
                path, Date(System.currentTimeMillis()),
                probabilitiesArray = junkImageClassifier.getClassificationProbabilities(byteBufferForImageData))
    }

    private fun copyBitmapIntoByteBuffer(imgData: ByteBuffer?, bitmap: Bitmap, intValues: IntArray) {
        if (imgData == null) {
            return
        }
        val startTime = System.currentTimeMillis()
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val pixelValue = intValues[pixel++]
                imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        Log.d("TIME_STAMPS", """Bitmap to ByteBuffer took: ${(System.currentTimeMillis() - startTime)}ms""")
    }

    private fun loadSampledBitmap(path: String, dimX: Int, dimY: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, dimX, dimY)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }


    companion object {
        private const val DIM_IMG_SIZE_X = 224
        private const val DIM_IMG_SIZE_Y = 224
        private const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0f
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3
        const val BYTE_BUFFER_SIZE: Int = 4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE
    }
}