package com.segregataur.db

import androidx.room.TypeConverter
import java.sql.Date

class DateTypeConverter {

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun fromLong(dateLong: Long): Date {
        return Date(dateLong)
    }
}