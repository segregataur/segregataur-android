package com.segregataur.db

import android.content.Context
import androidx.room.Room

object DatabaseManager {
    private var appDatabaseInstance: AppDatabase? = null

    fun getAppDatabaseInstance(applicationContext: Context): AppDatabase {
        if (appDatabaseInstance?.isOpen != true)
            appDatabaseInstance = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "segregataur")
                    .fallbackToDestructiveMigration()
                    .build()
        return appDatabaseInstance!!
    }
}