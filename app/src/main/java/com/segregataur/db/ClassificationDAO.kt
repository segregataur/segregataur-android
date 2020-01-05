package com.segregataur.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ClassificationDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(classificationDataEntity: ClassificationDataEntity)

    @Query("SELECT * FROM ClassificationData ORDER BY classificationTime DESC")
    fun getClassificationData(): LiveData<List<ClassificationDataEntity>>

    @Query("SELECT * FROM ClassificationData")
    fun getClassifiedMediaData(): List<ClassificationDataEntity>

    @Query("SELECT * FROM ClassificationData WHERE imageKey=:path")
    fun getClassifiedMediaData(path: String): ClassificationDataEntity

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateClassificationData(update: ClassificationDataEntity)

    @Delete
    fun remove(classificationDataEntities: List<ClassificationDataEntity>)
}
