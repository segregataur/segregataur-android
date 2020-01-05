package com.segregataur.db

import androidx.room.TypeConverter

class ProbabilityArrayTypeConverter {
    @TypeConverter
    fun fromString(labelsArrayDeepContent: String): FloatArray {
        return labelsArrayDeepContent
                .removePrefix("[")
                .removeSuffix("]")
                .split(",").map {
                    it.toFloat()
                }.toFloatArray()
    }

    @TypeConverter
    fun fromArray(labelsArray: FloatArray): String {
        return labelsArray.contentToString()
    }
}