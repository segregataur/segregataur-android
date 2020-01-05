package com.segregataur.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Date
import java.util.*

@Entity(tableName = "ClassificationData")
data class ClassificationDataEntity(
        @PrimaryKey
        var imageKey: String,
        var classificationTime: Date,
        var probabilitiesArray: FloatArray? = null,
        var userClassifiedJunk: Boolean? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassificationDataEntity

        if (imageKey != other.imageKey) return false
        if (!Arrays.equals(probabilitiesArray, other.probabilitiesArray)) return false
        if (userClassifiedJunk != other.userClassifiedJunk) return false
        if (classificationTime != other.classificationTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageKey.hashCode()
        result = 31 * result + (probabilitiesArray?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (userClassifiedJunk?.hashCode() ?: 0)
        result = 31 * result + classificationTime.hashCode()
        return result
    }
}