package com.segregataur.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
        entities = [ClassificationDataEntity::class],
        version = 10, exportSchema = false)
@TypeConverters(
        ProbabilityArrayTypeConverter::class,
        DateTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun classificationDAO(): ClassificationDAO
}