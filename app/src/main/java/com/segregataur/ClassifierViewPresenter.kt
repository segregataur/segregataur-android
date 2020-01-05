package com.segregataur

interface ClassifierViewPresenter {
    fun setSegregationOption(position: Int)
    fun startClassification()
    fun userCheckOverride(path: String, selected: Boolean)
    fun deleteJunkFiles(pathsToDelete: List<String>): Double
}