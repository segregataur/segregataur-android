package com.segregataur.supplier

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.*

abstract class BaseSupplier {

    fun getFilePathAndSizeMap(applicationContext: Context): Map<String, Long> {
        val returnMap = HashMap<String, Long>()
        getWhatsAppMediaFolderPath(
                applicationContext,
                Environment.getExternalStorageDirectory().absolutePath
        ).forEach {
            returnMap.putAll(getPathSizeMapFromFolderRecursively(it))
        }
        return returnMap
    }

    private fun getWhatsAppMediaFolderPath(
            applicationContext: Context,
            storageDir: String): Array<String> {

        val probableWhatsAppMediaPaths = arrayListOf("$storageDir${getMediaSuffix()}")
        val externalStoragePaths = getExternalStoragePath(applicationContext)
        for (path in externalStoragePaths) {
            val testPath = "$path${getMediaSuffix()}"
            if (File(testPath).exists())
                probableWhatsAppMediaPaths.add(testPath)
        }
        return probableWhatsAppMediaPaths.toTypedArray()
    }


    protected fun getPathSizeMapFromFolderRecursively(folderPath: String): HashMap<String, Long> {
        val returnMap = HashMap<String, Long>()
        val dirsList = Stack<File>()
        dirsList.push(File(folderPath))
        while (!dirsList.empty()) {
            val folderDir = dirsList.pop()
            if (folderDir.canRead())
                folderDir.list { dir, name ->
                    with(File(dir.absolutePath.plus("/$name"))) {
                        if (this.isFile) {
                            if (this.isHidden) {
                                false
                            } else {
                                returnMap[this.absolutePath] = this.length()
                                true
                            }
                        } else {
                            if (!this.isHidden) dirsList.push(this)
                            false
                        }
                    }
                }
        }
        return returnMap
    }

    abstract fun getMediaSuffix(): String
}