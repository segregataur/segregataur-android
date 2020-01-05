package com.segregataur.util

import android.content.Context
import android.preference.PreferenceManager

object SharedPreferencesUtil {

    fun getShowAskForRating(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ASK_FOR_RATING, true)
    }

    fun setShowAskForRatingFalse(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(ASK_FOR_RATING, false)
                .apply()
    }

    private const val ASK_FOR_RATING = "ask_for_rating"
}