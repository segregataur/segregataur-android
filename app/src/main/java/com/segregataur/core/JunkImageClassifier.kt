package com.segregataur.core

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class JunkImageClassifier(assets: AssetManager) {
    private val interpreter: Interpreter = Interpreter(loadModelFile(assets, RETRAINED_MODEL_FILE_PATH))
    private val labelProbArray = arrayOf(floatArrayOf(0f, 0f, 0f))

    fun getClassificationProbabilities(imageData: ByteBuffer): FloatArray {
        val startTime = System.currentTimeMillis()
        interpreter.run(imageData, labelProbArray)
        labelProbArray
                .contentDeepToString()
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
        Log.d("TIME_STAMPS", """Classification took: ${(System.currentTimeMillis() - startTime)}ms""")
        return labelProbArray[0]
    }

    fun clearResources() {
        interpreter.close()
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private const val RETRAINED_MODEL_FILE_PATH = "optimized_graph_0.75_1000.lite"
    }
}