package com.segregataur.supplier

import android.content.Context
import android.os.storage.StorageManager
import java.lang.reflect.InvocationTargetException


/**
 * Get external sd card path using reflection
 * @param mContext
 * @return
 */
fun getExternalStoragePath(mContext: Context): ArrayList<String> {

    val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val storageVolumeClazz: Class<*>?
    val externalVolumePaths = arrayListOf<String>()
    try {
        storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
        val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
        val getPath = storageVolumeClazz!!.getMethod("getPath")
        val result = getVolumeList.invoke(mStorageManager)
        (result as Array<*>).forEach {
            val storageVolumeElement = it
            val path = getPath.invoke(storageVolumeElement) as String
            externalVolumePaths.add(path)
        }
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    }
    return externalVolumePaths
}
