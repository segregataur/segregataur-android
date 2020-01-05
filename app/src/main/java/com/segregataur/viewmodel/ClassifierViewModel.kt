package com.segregataur.viewmodel

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.segregataur.core.ClassificationTask
import com.segregataur.core.JunkImageClassifier
import com.segregataur.data.ClassifierStateModel
import com.segregataur.data.SegregationOptions
import com.segregataur.data.SegregationOptions.BOTH
import com.segregataur.data.SegregationOptions.values
import com.segregataur.db.ClassificationDAO
import com.segregataur.db.ClassificationDataEntity
import com.segregataur.supplier.ImageFilesSupplier
import com.segregataur.supplier.VideoFilesSupplier
import com.segregataur.util.ByteBufferPool
import java.io.File
import java.sql.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ClassifierViewModel(private val junkImageClassifier: JunkImageClassifier,
                          private val classificationDAO: ClassificationDAO)
    : ViewModel() {

    private val payloadMap = HashMap<String, Long>()
    private var updateHead = 0
    private var junkCount = AtomicInteger(0)
    private var junkSize = AtomicLong(0L)

    val classifierStateModel: MutableLiveData<ClassifierStateModel> =
            MutableLiveData<ClassifierStateModel>().apply {
                this.value = ClassifierStateModel(
                        displayMessage = "\" Everything will be Sorted \""
                )
            }

    private var segregationOption = BOTH

    fun setSegregationOption(position: Int) {
        segregationOption = values()[position]
    }

    fun runBatchImageClassification(applicationContext: Context): LiveData<Triple<Array<String>, Array<String>, Boolean>> {
        val imagePathToSizeMap = ImageFilesSupplier().getFilePathAndSizeMap(applicationContext)
        val videoPathToSizeMap = VideoFilesSupplier().getFilePathAndSizeMap(applicationContext)

        when (segregationOption) {
            SegregationOptions.BOTH -> {
                payloadMap.putAll(imagePathToSizeMap)
                payloadMap.putAll(videoPathToSizeMap)
            }
            SegregationOptions.IMAGES -> payloadMap.putAll(imagePathToSizeMap)
            SegregationOptions.VIDEOS -> payloadMap.putAll(videoPathToSizeMap)
        }

        setStartState(totalToProcess = payloadMap.size)

        val terminationFlag = AtomicBoolean(false)
        Thread {
            val poolCount = 1
            val threadPool = Executors.newFixedThreadPool(poolCount)
            val byteBufferPool = ByteBufferPool(poolCount, ClassificationTask.BYTE_BUFFER_SIZE)


            val cachedMediaDataFromDB =
                    classificationDAO.getClassifiedMediaData()
                            .associateBy({
                                it.imageKey
                            }, {
                                Pair(
                                        it.probabilitiesArray,
                                        it.userClassifiedJunk
                                )
                            })

            val tasksList =
                    payloadMap
                            .filterNot {
                                cachedMediaDataFromDB.containsKey(it.key)
                            }
                            .map {
                                ClassificationTask(
                                        it.key,
                                        byteBufferPool,
                                        junkImageClassifier,
                                        classificationDAO
                                )
                            }
            try {
                threadPool.invokeAll(tasksList)
                threadPool.shutdown()
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
            } finally {
                terminationFlag.set(true)
                setEndState(junkCount.get(), junkSize.get())
            }
        }.start()

        return Transformations.map(
                classificationDAO.getClassificationData()
        ) { fullList ->

            var reformat = false
            val requiredList = fullList.filter {
                payloadMap.containsKey(it.imageKey)
            }

            if (updateHead > requiredList.size) {
                updateHead = 0
                reformat = true
            }

            val junkArray = requiredList.reversed().subList(updateHead, requiredList.size)
                    .filter { isJunk(it.probabilitiesArray, it.userClassifiedJunk) }
                    .map { it.imageKey }
                    .toTypedArray()

            val safeArray = requiredList.reversed().subList(updateHead, requiredList.size)
                    .filterNot { isJunk(it.probabilitiesArray, it.userClassifiedJunk) }
                    .map { it.imageKey }
                    .toTypedArray()

            junkSize.addAndGet(junkArray.foldRight(0L) { path, acc ->
                junkCount.incrementAndGet()
                acc + (payloadMap[path] ?: 0L)
            })

            updateHead = requiredList.size

            if (terminationFlag.get())
                setEndState(junkCount.get(), junkSize.get())
            else
                setIntermediateState(updateHead, junkSize.get())

            Triple(junkArray, safeArray, reformat)
        }
    }

    fun userClassification(path: String, junk: Boolean) {
        Thread {
            val data = classificationDAO.getClassifiedMediaData(path)
            data.userClassifiedJunk = junk
            classificationDAO.updateClassificationData(data)
        }.start()
    }

    fun deleteFiles(context: Context, pathsToDelete: List<String>): Double {
        var deletedBytes = 0L
        Thread {
            classificationDAO.remove(pathsToDelete.map { ClassificationDataEntity(it, Date(System.currentTimeMillis())) })
        }.start()
        for (path in pathsToDelete) {
            File(path).also { deletedBytes += it.length() }.delete()
        }
        MediaScannerConnection.scanFile(context, pathsToDelete.toTypedArray(),
                null) { _, _ ->
            Log.i("Complete Rescan", "Done")
        }
        return deletedBytes.toDouble()
    }

    private fun isJunk(probabilitiesArray: FloatArray?, userClassifiedJunk: Boolean?): Boolean {
        return userClassifiedJunk ?: isClassificationHintingJunk(probabilitiesArray)
    }

    private fun isClassificationHintingJunk(classificationProbabilities: FloatArray?): Boolean {
        // this method will get really complex in the future with multiple model class
        return when {
            classificationProbabilities == null -> false
            classificationProbabilities[JUNK_PROB_INDEX] > JUNK_CLASSIFICATION_THRESHOLD -> true
            else -> false
        }
    }

    private fun setStartState(totalToProcess: Int) {
        classifierStateModel.value =
                classifierStateModel.value!!.also {
                    it.classificationStarted = true
                    it.totalItemsToProcess = totalToProcess
                }
    }

    private fun setIntermediateState(processed: Int, junkSize: Long) {
        val currentState = classifierStateModel.value!!
        if (!currentState.classificationStarted || currentState.classificationComplete) return
        classifierStateModel.value =
                currentState.also { stateModel ->
                    stateModel.itemsProcessed = processed
                    stateModel.junkSize = junkSize
                    stateModel.displayMessage =
                            "Analyzed $processed of ${stateModel.totalItemsToProcess}"
                }
    }

    private fun setEndState(junkItemCount: Int, junkSize: Long) {
        classifierStateModel.postValue(
                classifierStateModel.value!!.also {
                    it.classificationComplete = true
                    it.junkSize = junkSize
                    it.displayMessage =
                            """$junkItemCount found to be junk"""
                })
    }

    override fun onCleared() {
        super.onCleared()
        junkImageClassifier.clearResources()
    }

    companion object {
        private const val JUNK_CLASSIFICATION_THRESHOLD = 0.60f
        private const val JUNK_PROB_INDEX = 1
    }
}